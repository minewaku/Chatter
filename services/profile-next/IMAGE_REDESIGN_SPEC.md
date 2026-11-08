# profile-next: image redesign spec

Implementation-oriented version of `IMAGE_REDESIGN_PLAN.md` (that file keeps the reasoning, rejected alternatives and
research notes; where it conflicts with this file, **this file wins**). This one is organized by topic and contains what
is needed to build it.
Status: not implemented. Scope: `services/profile-next` only.

Revision: the user id is back in every storage key (`.../{userId}/{slot}/{imageId}`). Ownership therefore comes from the
signed `public_id`, and the `pending_image_upload` ticket table of the earlier draft is gone.

## 1. Goal

Replace `StoredImage(publicId, url)` with a model where the profile stores only an image id per slot, storage keys and
URLs are derived, and unconfirmed uploads cannot become permanent orphans. Provider (Cloudinary) details stay in the
infrastructure module. No image metadata is persisted: the `profile` row holds the two image ids and nothing else.

## 2. Terms

| Term | Meaning |
|---|---|
| `ImageId` | UUIDv7, produced by the existing `UuidV7IdGenerator`; ordering = recency |
| slot | `AVATAR` or `BANNER`; path segment `avatars` / `banners` |
| temporary key | `chatter/temp/profile/{userId}/{slot}/{imageId}`, where every client upload lands |
| permanent key | `chatter/profile/{userId}/{slot}/{imageId}`, reached only by promotion |
| promotion | server-side rename temporary -> permanent, driven by `pending_image_promotion` |
| promoted | the image has no row in `pending_image_promotion` |

`chatter` is the configurable root; use a different root per environment (for example `chatter-dev`). The user id comes
right after the root segment so that one prefix (`{root}/profile/{userId}/` and `{root}/temp/profile/{userId}/`) covers
everything a user owns.

**Ownership rule:** the server signs only `public_id`s that contain the authenticated caller's own user id. Cloudinary
rejects an upload whose `public_id` differs from the signed one, and `overwrite=false` makes a permission usable once.
So the user id parsed from a webhook's `public_id` is trustworthy.

## 3. Domain model (core)

```java
public record ImageId(UUID value) implements Comparable<ImageId> { /* compareTo = value.compareTo */ }
public record ImageDimensions(int width, int height) { /* both positive */ }   // used by ConfirmedUpload + ImagePolicy only

public record Profile(UserId userId, DisplayName displayName, Bio bio,
                      ImageId avatar, ImageId banner,          // nullable; nothing else stored
                      Instant createdAt, Instant updatedAt) { }
```

- `StoredImage` is deleted. `ImageSlot` stays but loses `folder()`; the folder names live in the adapter.
- Read model: each image is `ImageRef(ImageId id, boolean promoted)`; the write model has no promotion state. The user
  id for URL building is the profile's own.
- `ConfirmedUpload` (from `ImageStorage.verify`) = `UserId`, `ImageSlot`, `ImageId`, format, bytes, `ImageDimensions`.
  User, slot and id are parsed from the signed temporary `public_id`. No raw public id, no url.
- Staleness: a confirmed id is applied only if it is newer than the stored id of that slot (`ImageId.compareTo`).

## 4. Storage and Cloudinary

### Keys
Adapter-internal `ImageKey`: `of(root, userId, slot, imageId)` for both roots, and `parse(publicId)` for temporary keys
(returns user + slot + id, rejects anything else). Core never sees a path.

### Upload permission (signed parameters)
| Parameter | Value |
|---|---|
| `public_id` | temporary key of the caller (`userId` from the authenticated principal) |
| `overwrite` | `false` (Cloudinary defaults to `true` for signed uploads; the current adapter does not set it) |
| `transformation` | `c_limit,w_{max},h_{max}`; avatar 1024, banner 2048 (incoming transformation, applied before storing) |
| `allowed_formats`, `timestamp`, `notification_url` | as today |
| `tags`, `context` | not used |

Everything sent except `file`, `cloud_name`, `resource_type`, `api_key` is part of the signature.

### Delivery URL
`ImageUrlResolver(userId, slot, imageId, promoted, variant)` -> `https://res.cloudinary.com/{cloud}/image/upload/{transformation}/{key}`
with key from the permanent root when promoted, else the temporary root. Variants are transformations, for example
`c_fill,w_128,h_128,g_face,f_auto` (avatar thumb). The UI crops before upload; delivery `c_fill` keeps display
correct for any shape. Images are immutable (new upload = new id), so `Cache-Control: immutable` is safe.
User ids appear in public image URLs; they are public information in this system, so this is accepted.

### Provider operations needed on the `ImageStorage` port
`permit(userId, slot, id)` (temporary key), `verify` (webhook), `promote(userId, slot, id)` (rename, `overwrite=false`,
no `notification_url`; on any failure look the permanent key up with the Admin API `resource` and count it as done if
present), `remove(userId, slot, id)` (destroy permanent and temporary key; missing = success), `list(cursor)` (permanent
prefix only, 500 per page; each entry parsed into user + slot + id).

## 5. Database (edit `V1__create_profile.sql` in place; no data to migrate)

```sql
-- profile: replace avatar_public_id/avatar_url/banner_public_id/banner_url (and their CHECKs and indexes) with
avatar_image_id uuid NULL,
banner_image_id uuid NULL,
-- no index on them: the sweeper resolves the owner from the key and loads the profile by primary key

-- pending_image_removal, re-keyed
image_id uuid PRIMARY KEY, user_id bigint NOT NULL, slot text NOT NULL,
-- attempts, next_attempt_at, created_at, reported_at unchanged (due index unchanged)
-- user_id is required: the profile row may already be deleted when the worker runs (account deletion)

CREATE TABLE pending_image_promotion (
    image_id        uuid        PRIMARY KEY,
    user_id         bigint      NOT NULL,
    slot            text        NOT NULL,
    attempts        integer     NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL,
    created_at      timestamptz NOT NULL,
    reported_at     timestamptz NULL
);
CREATE INDEX pending_image_promotion_due_idx ON pending_image_promotion (next_attempt_at) WHERE reported_at IS NULL;
```

There is no ticket table and no images table.

## 6. Behaviour

### 6.1 Request an upload (`RequestImageUpload`)
1. Generate `ImageId`. 2. Sign the permission (section 4) for the authenticated user and the requested slot. 3. Return
url / method / fields / expiry. Nothing is written to the database. (No rate limit for now; added after the core is
finished.)

### 6.2 Webhook (`ConfirmImageUpload`) -- always responds `200`
Controller catches everything: an unexpected failure is logged at error level and still answers `200`; an invalid
signature, a malformed body and non-`upload` notification types (for example a rename notification) are ignored with `200`.
Steps: verify signature and freshness; parse the temporary `public_id` into user, slot and id (anything else ->
`IGNORED`); policy on format, bytes and dimensions. Then, in one transaction, create the user's profile row if it does
not exist (`INSERT (user_id, created_at, updated_at) ... ON CONFLICT DO NOTHING`, as `replaceImage` does today) and lock
it (`SELECT ... FOR UPDATE`). The create is required: a profile row exists only after the user's first write, so a new
user may upload an image before ever saving a name or bio. Outcomes:

| Case | Writes | Result |
|---|---|---|
| Policy fails | none | `FILE_NOT_ALLOWED` (file expires by TTL) |
| Newer than stored id for the slot | set `profile.{slot}_image_id`; enqueue previous id in `pending_image_removal` (with `user_id`); insert `pending_image_promotion` (with `user_id`) | `APPLIED` |
| Not newer / repeat | none | `IGNORED` (file expires by TTL) |

Rejected, stale and abandoned uploads get no removal row; the Cloudinary purge (section 8) handles them.

### 6.3 Promotion worker (`PromotePendingImages`, polling)
Same pattern as the removal worker: scheduler, `FOR UPDATE SKIP LOCKED`, lease by pushing `next_attempt_at`, backoff,
`attempts`. Per row: `promote`; success -> delete the row. Past the horizon (default 2 h): set `reported_at`, clear the
slot with `UPDATE profile SET {slot}_image_id = NULL WHERE user_id = :user AND {slot}_image_id = :id`, log an error.

### 6.4 Read (`GetProfile`)
Query returns `ImageRef(id, promoted = NOT EXISTS promotion row)` per slot (join on the primary key); the resolver builds
one URL from the profile's user id. The UI shows its default asset when there is no URL or the image fails to load. After
a rename the old URL is invalid, so the temp URL can fail for the very short moment between the rename and the row
deletion.

### 6.5 Replace and remove
`RemoveObsoleteImages` (existing worker, re-keyed): `remove(userId, slot, id)` deletes both keys; retries and
`reported_at` as today.

### 6.6 Account deletion (`DeleteProfileData`, one transaction)
Delete the profile row (capturing its image ids) -> enqueue those ids in `pending_image_removal` with the user id ->
delete their promotion rows. A webhook racing the deletion either lands before it (the deletion captures its id) or
after it, in which case 6.2 recreates a profile row holding that image (known ghost row, section 13). Provider-side prefix delete
(`{root}/profile/{userId}/`, `{root}/temp/profile/{userId}/`) is possible because of the key layout but is **not**
planned: no mass deletes are expected, and the sweeper and TTL cover leftovers. If added later, note the Admin API rate
limit and the 1000-assets-per-call cap.

### 6.7 Backstop sweeper (`SweepUnreferencedImages`, existing)
List `chatter/profile/` only; skip files younger than the grace period, referenced by their owner's profile (owner parsed
from the key, profiles loaded by primary key), or with an active removal row; delete the rest. Never touches
`chatter/temp/`. Optional later optimization: incremental Search-API window
`public_id:chatter/profile/* AND created_at:[{watermark} TO 1d]` instead of a full listing.

## 7. Configuration (`profile-next.images.*`, names proposed)

| Property | Default | Use |
|---|---|---|
| `root` | `chatter` | key root; differs per environment |
| `max-dimension.avatar` / `.banner` | 1024 / 2048 | `c_limit` bound |
| `promotion.horizon` | `PT2H` | when promotion gives up |
| `promotion.*` backoff, worker interval | mirror the removal worker | retry timing |

Existing keys unchanged (`cloud-name`, `api-key`, `api-secret`, `webhook-url`, `signature-algorithm`, allowed formats,
permission lifetime, webhook tolerance).

## 8. Cloudinary console setup (owner)

The account uses **dynamic folder mode**: `public_id` prefixes still work for the Search/Admin APIs and for the purge,
and per-user paths do not create a folder per user.

MediaFlows PowerFlow "delete temporary UGC assets" (the Cloudinary guide of that name):
- Search expression: `public_id:chatter/temp/* AND created_at < 1d`; it must never match `chatter/profile/*`.
- Recurring hourly; action Delete Asset with Invalidate; cap assets per run.
- Use a prefix, not the guide's tag example: tags very likely stay on a renamed asset and would delete promoted images.
- Invariant: TTL (1 day) far above the promotion horizon (2 hours).
- Per environment: matching root and expression. Do not set a console-wide Notification URL for the webhook.
- Untested by choice. Fallback if the prefix is not accepted: sign a purge-only tag on temporary uploads, select
  `tags:{that tag} AND created_at < 1d`, and remove the tag in the promotion step.
- Record the expression and schedule in `infrastructure/components/vault/docs/runbooks/profile-next-cloudinary.md`.

## 9. Failure modes

| What goes wrong | Result | Cleaned by |
|---|---|---|
| Webhook lost or its handling failed (never retried) | File in temp, no profile change | TTL; user re-uploads |
| Webhook after the account was deleted (upload in flight at deletion) | Ghost profile row holding the image | none yet (section 13) |
| Policy rejection, stale upload, repeat | File in temp, unreferenced | TTL |
| Rename fails transiently | Profile points at the temp file; URL uses the temp root | Worker retries |
| Process dies after rename, before row delete | Next attempt finds the permanent key | Worker |
| Promotion exhausted | Slot cleared, `reported_at`, error log | TTL |
| Removal races a promotion of the same id | Possible permanent orphan (rare) | Backstop sweeper |
| Removal retries exhausted | `reported_at` set; file stays | Backstop sweeper |

## 10. Code changes by module

**Core:** remove `model/StoredImage`; add `model/ImageId`, `model/ImageDimensions`; `model/Profile`; `model/ImageSlot`
(drop `folder()`); `port/ImageStorage` (section 4 operations, new `ConfirmedUpload`, `StoredImageInfo` with parsed
user + slot + id); `port/repository/`: `ProfileRepository` (replace image + promotion insert + clear-slot-if-equal + load
for update), `PendingImageRemovalRepository` (rows carry `userId`), new `PendingImagePromotionRepository` (rows carry
`userId`); `port/query/ProfileQueryRepository` (`ImageRef`); `application/command/`: `RequestImageUploadService`,
`ConfirmImageUploadService`, `ImagePolicy`, `RemoveObsoleteImagesService`, `SweepUnreferencedImagesService`,
`DeleteProfileDataService`, `SaveProfileService`, new `PromotePendingImagesService` (+ policy) with
`api/command/PromotePendingImagesUseCase`; `application/query/GetProfileService`.

**Infrastructure:** `adapter/outbound/image/storage/cloudinary/` (`CloudinaryImageStorage`, `CloudinaryClient`,
`SdkCloudinaryClient` with `rename` and `resource`, new `ImageKey`, `ImageProperties`), new `ImageUrlResolver`, JDBC:
`JdbcProfileRepository`, `JdbcProfileQueryRepository`, `JdbcPendingImageRemovalRepository`, new
`JdbcPendingImagePromotionRepository`, a scheduler for promotion (mirror the removal one), `V1__create_profile.sql`.

**Presentation:** `image/web/ImageUploadController` (always `200`), `shared/error/ApiExceptionHandler` for that path.
**Start:** `application.yml`. **Docs:** the runbook above.

## 11. Suggested order

1. Independent fixes in the current code: `overwrite=false`; webhook `200`.
2. Value objects and `ImageKey` with tests.
3. `V1` rewrite, repositories and fakes.
4. `ImageStorage` port + Cloudinary adapter (`rename`, `resource`, signing, parsing).
5. Request / confirm / promote / remove / delete-data / sweep services.
6. Read side and `ImageUrlResolver`; scheduler and properties.
7. MediaFlows setup and runbook; end-to-end run against Cloudinary.

## 12. Tests

Update: `ConfirmedUploadTest`, `ImageSlotTest`, `CloudinaryImageStorageTest`, `JdbcProfileRepositoryTest`,
`JdbcProfileQueryRepositoryTest`, `JdbcPendingImageRemovalRepositoryTest`, command-service tests, `FakeRemovalQueue`,
`ImageFlowE2eTest`, `AccountDeletionE2eTest`. Add: `ImageId` ordering; `ImageDimensions` validation; `ImageKey` (format
with user id, parse, reject); resolver (both roots, variants, user id); promotion worker (success, retry after "permanent
exists", exhaustion clears the slot); `JdbcPendingImagePromotionRepository`; confirm for a user with no profile row yet
(creates it, first upload before any name or bio save) / unparsable key; webhook always `200`.

## 13. Accepted trade-offs and open items

- Lost or failed notifications are not retried or alerted; the user re-uploads.
- A promotion that gives up loses both the new and the previous image.
- Rare removal-vs-promotion race can leave a permanent orphan (backstop sweeper).
- Ghost profile row: if a webhook arrives after the account-deleted event was processed, the lazy create in 6.2 makes a
  profile row (and keeps its image) for a deleted user. Needs an upload in flight at the moment of deletion; accepted
  for now (same as the current code). Hardening later: a small tombstone table of deleted user ids written by
  `DeleteProfileData` and checked in 6.2 (deleted -> `IGNORED`). Creating the row eagerly on signup is not an
  alternative: nothing in this service consumes an account-created event.
- Provider-side per-user prefix delete is possible (key layout) but not planned; if added, mind the Admin API rate limit
  and the 1000-assets-per-call cap.
- No rate limit on `RequestImageUpload` yet; temp is bounded by the 1-day TTL. Add after the core is finished.
- User ids are visible in public image URLs (accepted: public information here). Any other service rendering an avatar
  needs `userId`, `slot` and `imageId`.
- Ownership relies on the server signing only the caller's own `public_id` (section 2); `RequestImageUpload` must never
  take the user id from the request body.
- Not verified: how the Cloudinary plan counts incoming transformations; MediaFlows availability and the prefix
  expression.
- The working tree already has uncommitted edits in this package; reconcile them first.
