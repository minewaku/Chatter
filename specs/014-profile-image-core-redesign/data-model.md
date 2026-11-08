# Data Model: Profile Image Redesign (core layer)

Types are described by what they hold and which rules they enforce, not by code. Package `…profile.internal.model` unless noted. "Nullable" means the value may be absent, never an empty placeholder.

## Value types (model package)

| Type | Holds | Rules |
|---|---|---|
| `UserId` (existing) | `long` | positive |
| `ImageSlot` (existing, changed) | `AVATAR`, `BANNER` | exposes no folder name (FR-005) |
| `ImageId` (new) | a UUID | not null; `of(String)` rejects text that is not a UUID; ordered by the UUID's natural order, which for UUIDv7 follows creation time (research D1); `a.compareTo(b) > 0` means `a` is newer |
| `ImageDimensions` (new) | `width`, `height` | both greater than zero, else `IllegalArgumentException` |
| `OwnedImage` (new) | `UserId`, `ImageSlot`, `ImageId` | none beyond non-null parts; the unit every port speaks |
| `ImageRef` (new, read model) | `ImageId`, `promoted` | `promoted` true once no promotion record exists for the id |

## Aggregates and views

**Profile** (write model, `model`): `userId`, `displayName` (nullable `DisplayName`), `bio` (`Bio`, empty by default), `avatar` (nullable `ImageId`), `banner` (nullable `ImageId`), `createdAt`, `updatedAt` (both null while no row exists). `Profile.defaultsFor(userId)` stays. It carries **no** path, address, format, size or dimensions. Helper: `imageIn(slot)` returns the slot's `ImageId` or null.

**ProfileView** (read model, nested in `ProfileQueryRepository`): `userId`, `displayName`, `bio`, `avatar` and `banner` as nullable `ImageRef`, `createdAt`, `updatedAt`. `defaultsFor(userId)` gives the same defaults as `Profile`.

## Port data (nested records)

| Record | Lives in | Fields |
|---|---|---|
| `UploadPermission` | `ImageStorage` | `uploadUrl`, `method`, `fields` (map), `expiresAt` |
| `ConfirmedUpload` | `ImageStorage` | `userId`, `slot`, `imageId`, `format`, `bytes`, `ImageDimensions`; `image()` returns the `OwnedImage` |
| `StoredImageInfo` | `ImageStorage` | `OwnedImage`, `createdAt` (permanent images only) |
| `StoredImagePage` | `ImageStorage` | `items`, `nextCursor` (null on the last page) |
| `DeletedProfile` | `ProfileRepository` | nullable `avatar` and `banner` `ImageId` |
| `PendingRemoval` | `PendingImageRemovalRepository` | `OwnedImage`, `attempts` |
| `PendingPromotion` | `PendingImagePromotionRepository` | `OwnedImage`, `attempts`, `createdAt` |

Removed: `StoredImage`, `ProfileRepository.Replacement`, `ImageStorage.ConfirmedUpload.publicIdMatchesOwner()`, `PendingRemoval(publicId, …)`.

## Policy values (application.command)

| Type | Fields | Behaviour |
|---|---|---|
| `ImagePolicy` | `maxBytesAvatar`, `maxBytesBanner`, `maxDimensionAvatar`, `maxDimensionBanner`, `allowedFormats`, `permissionLifetime`, `webhookTolerance` | `maxBytes(slot)`, `maxDimension(slot)`, `allows(format)` case-insensitive, `permits(ConfirmedUpload)` = format allowed and bytes ≤ limit and width ≤ limit and height ≤ limit |
| `RemovalPolicy` (existing) | `maxRetries`, `delays`, `pollBatchSize`, `lease` | unchanged |
| `PromotionPolicy` (new) | `horizon`, `delays`, `pollBatchSize`, `lease` | `delayBeforeRetry(n)` as removal; `expired(createdAt, now)` true when `now ≥ createdAt + horizon` |
| `SweepPolicy` (existing) | `gracePeriod` | unchanged |

## Queues (state)

Both queues are keyed by `ImageId`; one record per image; a duplicate enqueue is ignored and does not reset attempts.

**Promotion record**: `ImageId`, `UserId`, `ImageSlot`, `attempts`, `nextAttemptAt`, `createdAt`, `reportedAt` (nullable).

```text
                 enqueue (on APPLIED)
                        |
                        v
                   [ DUE ] --takeDue(lease)--> [ LEASED: nextAttemptAt pushed by lease ]
                      ^                               |
                      | markFailed(next = now+delay)  |-- promote ok ------> deleted  (image is now "promoted")
                      +-------------------------------|-- promote fails, not expired
                                                      |
                                                      +-- promote fails, expired --> clear slot if still equal,
                                                                                    then [ REPORTED ] (never taken again)
   cancelled (previous image replaced) ----------------------------------------------> deleted
   account deleted (deleteByUserId) -------------------------------------------------> deleted
```

**Removal record**: same shape without `createdAt` use; fails past `maxRetries` to `REPORTED`; success deletes the record.

## Confirm-upload decision table

Evaluated in this order after a notification verified. "Locked" means the owner's profile was taken `FOR UPDATE` first.

| # | Condition | Writes | Result |
|---|---|---|---|
| 1 | verification fails (signature, freshness, shape, key not a temporary-upload key) | none | throws rejected, reason `INVALID_SIGNATURE` |
| 2 | `ImagePolicy.permits` is false | none | `FILE_NOT_ALLOWED` |
| 3 | locked profile does not exist | none | `IGNORED` |
| 4 | slot holds an id that is equal to or newer than the confirmed id | none | `IGNORED` |
| 5 | slot empty | set slot; enqueue promotion | `APPLIED` |
| 6 | slot holds an older id P | set slot; enqueue removal of P; cancel promotion of P; enqueue promotion | `APPLIED` |

## Account deletion (one transaction)

1. `deleteByUserId` returns the captured avatar and banner ids (or nothing).
2. For each captured id, enqueue removal as `OwnedImage(user, slot, id)`.
3. `deleteByUserId` on the promotion queue for the user.
4. Repeat is a no-op: step 1 returns nothing, step 3 finds nothing.

## Read: promoted flag

`ImageRef.promoted` is `true` exactly when no promotion record exists for that `ImageId`. In the core this is a property of the query port's contract, proved on the fake; the JDBC spec proves it as `NOT EXISTS` on the primary key.
