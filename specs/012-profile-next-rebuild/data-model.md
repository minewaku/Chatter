# Data Model: Profile Service Rebuild (profile-next)

Schema `profile_next` in the shared `chatter` database, created by Flyway `V1__create_profile.sql`. Entities map to spec 012 *Key Entities*. Spec 013's `identity_access.outbox` is a different service's table and is not touched.

## Tables

### `profile` (Profile and its two Image references)

| Column | Type | Rules |
|---|---|---|
| `user_id` | `bigint` PK | The account id from the access token `sub`. No foreign key (the account lives in another service). |
| `display_name` | `text NOT NULL DEFAULT ''` | 0–32 user-perceived characters after trimming (R3); empty only for rows created by an image confirmation. `CHECK (char_length(display_name) <= 1024)` as a storage guard. |
| `bio` | `text NOT NULL DEFAULT ''` | 0–190 user-perceived characters. Same guard. |
| `avatar_public_id` | `text NULL` | Provider public id `chatter/profile/avatars/{userId}/{uploadId}`. |
| `avatar_url` | `text NULL` | Public delivery address. Null together with `avatar_public_id`. |
| `banner_public_id` | `text NULL` | `chatter/profile/banners/{userId}/{uploadId}`. |
| `banner_url` | `text NULL` | Null together with `banner_public_id`. |
| `created_at` | `timestamptz NOT NULL` | Set from the injected clock on insert. |
| `updated_at` | `timestamptz NOT NULL` | Set from the injected clock on every real change; unchanged on a no-op save. |

Constraints: `CHECK ((avatar_public_id IS NULL) = (avatar_url IS NULL))`, same for banner. No index beyond the primary key: every read and write is by `user_id`, and the cleanup's reference check is by public id over a bounded page (`WHERE avatar_public_id = ANY(:ids) OR banner_public_id = ANY(:ids)`), so two partial indexes `ON profile (avatar_public_id) WHERE avatar_public_id IS NOT NULL` and the same for banner are added for it.

The upload id is the last path segment of the public id and is a UUIDv7; the confirmation compares it as text (R5). It is derived, not a column.

State: a row does not exist until the first text save or the first image confirmation. A missing row is presented as defaults (FR-001). The only transitions are insert, update, and delete-by-user (account-deleted notice). There is no deleted or locked state.

### `pending_image_removal` (Pending image removal)

| Column | Type | Rules |
|---|---|---|
| `public_id` | `text` PK | The provider file to delete. A second insert for the same id is ignored (`ON CONFLICT DO NOTHING`). |
| `slot` | `text NOT NULL` | `AVATAR` or `BANNER`, for logging and tests. |
| `attempts` | `integer NOT NULL DEFAULT 0` | Failed removal attempts so far. |
| `next_attempt_at` | `timestamptz NOT NULL` | Due when `<= now`. Starts at insert time. |
| `created_at` | `timestamptz NOT NULL` | |
| `reported_at` | `timestamptz NULL` | Set once, when the retry limit is exhausted; the worker skips rows where it is set. |

Index: `(next_attempt_at) WHERE reported_at IS NULL`, for the worker's due-rows query.

Lifecycle: inserted in the same transaction as the change that made the file obsolete; deleted when the provider confirms removal (a "not found" answer counts as confirmed); kept with `reported_at` set after the retry limit, for an operator.

## Core model (pure Java, no framework types)

| Type | Kind | Notes |
|---|---|---|
| `UserId` | value object | Positive `long`. Built from the token or the notice. |
| `DisplayName`, `Bio` | value objects | Validate, trim and count as in R3. Built only from user input. |
| `ImageSlot` | enum | `AVATAR(5 MB)`, `BANNER(10 MB)`, with the allowed formats `jpg`, `png`, `webp`. Limits are configuration; the enum holds the slot, not the number. |
| `StoredImage` | record | `publicId`, `url`, `uploadId` (derived), `slot`. |
| `Profile` | record (read side) | `userId`, `displayName`, `bio`, `avatar`, `banner`, `createdAt`, `updatedAt`. A `Profile.defaultsFor(userId)` factory builds the empty one. |
| `ProfileEdit` | record | `Optional<DisplayName>`, `Optional<Bio>`; the command's validated shape. |

No aggregate root, no event list, no state machine (FR-017).

## Ports (core) and their single adapters

| Port | Package in core | Adapter | Package in infrastructure |
|---|---|---|---|
| `ProfileRepository` (`applyEdit`, `replaceImage`, `deleteByUserId`) | `profile.internal.port.repository` | `JdbcProfileRepository` | `adapter.outbound.profile.write.jdbc` |
| `ProfileQueryRepository` (`findByUserId`, `findReferencedPublicIds`) | `profile.internal.port.query` | `JdbcProfileQueryRepository` | `adapter.outbound.profile.read.jdbc` |
| `PendingImageRemovalRepository` (`enqueue`, `takeDue` with a lease, `markRemoved`, `markFailed`, `findActivePublicIds`, `clear`) | `profile.internal.port.repository` | `JdbcPendingImageRemovalRepository` | `adapter.outbound.image.removal.jdbc` |
| `ImageStorage` (`permit`, `verify`, `remove`, `list`) | `profile.internal.port` | `CloudinaryImageStorage` | `adapter.outbound.image.storage.cloudinary` |
| `UniqueStringIdGenerator` | `shared.port` | `UuidV7IdGenerator` | `adapter.outbound.shared.id` |

`replaceImage` returns the previous image (if any) and whether the slot changed, so the use case can enqueue the right file in the same transaction. `permit`, `verify` and `list` return core records (`UploadPermission`, `ConfirmedUpload`, `StoredImagePage`) that carry no Cloudinary types.

## Use cases (single-method handlers in `profile.api`)

| Use case | Kind | Input → output | Requirement |
|---|---|---|---|
| `GetProfileUseCase` | query | `userId` → `ProfileView` (defaults when no row) | FR-001, FR-002 |
| `SaveProfileUseCase` | command | `userId`, optional name, optional bio → `changed` | FR-003 to FR-005, FR-014 |
| `RequestImageUploadUseCase` | command | `userId`, slot → `UploadPermission` | FR-007 |
| `ConfirmImageUploadUseCase` | command | raw body bytes, signature, timestamp → outcome `APPLIED`, `IGNORED` or `FILE_NOT_ALLOWED` | FR-008, FR-009 |
| `DeleteProfileDataUseCase` | command | `eventId`, `userId`, `occurredAt` → none | FR-011, FR-026 |
| `RemoveObsoleteImagesUseCase` | command (scheduled) | none → counts | FR-009 |
| `SweepUnreferencedImagesUseCase` | command (scheduled) | none → counts | FR-010 |

Exceptions in `profile.api.exception`: `InvalidProfileValueException` (rule and field), `UploadConfirmationRejectedException` (bad signature, mismatched user), `ImageStorageUnavailableException` (permission or removal cannot reach the provider).

## Message shapes owned here

The account-deleted notice record in presentation: `eventId` (string), `userId` (string, parsed to `long`), `occurredAt` (ISO-8601 instant). Unknown fields ignored. This is `profile-next`'s own copy of the shape in `contracts/account-deleted-event.md`.
