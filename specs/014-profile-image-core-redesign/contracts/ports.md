# Contract: outbound ports (core → adapters)

Binding for the adapters written in later specs and for the fakes written here. Signatures are given in prose with types; exact Java is a task-level detail. All ports live under `…profile.internal.port`. No signature exposes a provider, persistence or web type. `OwnedImage` = (`UserId`, `ImageSlot`, `ImageId`).

## `ImageStorage`

| Operation | Input | Output | Contract |
|---|---|---|---|
| `permit` | `OwnedImage`, `now` | `UploadPermission` | Signs an upload that can only target the temporary location of exactly this owner, slot and id, and can be used once. Throws `ImageStorageUnavailableException` / `ImageStorageRateLimitedException` on provider trouble |
| `verify` | raw body bytes, signature, timestamp, `now` | `Optional<ConfirmedUpload>` | Empty when the signature is wrong, the timestamp is stale, the body is malformed, the notification is not an upload notification, or the location does not parse as a temporary-upload location of a known slot with a valid user id and image id. Otherwise owner, slot and id come **only** from that signed location. Never throws for bad input |
| `promote` | `OwnedImage` | nothing | Moves the image from temporary to permanent storage without overwriting. If the permanent copy already exists it returns normally. Any other failure throws |
| `remove` | `OwnedImage` | nothing | Deletes the permanent and the temporary copy. A copy that is already gone counts as success. Any other failure throws |
| `list` | cursor (null = start) | `StoredImagePage` | Returns **permanent** images only, each with `OwnedImage` and creation time, plus the next cursor (null at the end). Never returns temporary images |

## `ProfileRepository` (write)

| Operation | Contract |
|---|---|
| `applyEdit(userId, displayName?, bio?, now)` | Unchanged: one atomic upsert; null part left as is; returns true when a row was written, false when values equal what is stored |
| `findForUpdate(userId)` | Returns the profile and holds an exclusive lock on it until the surrounding transaction ends; empty when there is no row. Must be called inside a transaction |
| `setImage(userId, slot, imageId, now)` | Sets one slot and `updatedAt` on an existing row. Not called when there is no row |
| `clearImageIf(userId, slot, imageId, now)` | Sets the slot to null only if it currently equals `imageId`; returns whether it did; does nothing (returns false) when there is no row |
| `deleteByUserId(userId)` | Deletes the row and returns `DeletedProfile` (the ids it held); empty when there was no row |

Removed: `replaceImage`, `Replacement`.

## `ProfileQueryRepository` (read)

| Operation | Contract |
|---|---|
| `findByUserId(userId)` | `Optional<ProfileView>`; each present image is an `ImageRef` whose `promoted` is true exactly when no promotion record exists for the id |
| `findReferenced(Collection<OwnedImage>)` | The subset whose id is currently held in the stated slot of the stated owner's profile |

## `PendingImageRemovalRepository`

| Operation | Contract |
|---|---|
| `enqueue(OwnedImage, now)` | Adds a due record; ignored if the id is already queued (attempts not reset) |
| `takeDue(now, limit, lease)` | Up to `limit` unreported records with `nextAttemptAt ≤ now`, oldest first; each returned record is hidden from other callers for `lease` (its `nextAttemptAt` is pushed). Two concurrent callers never receive the same record |
| `markRemoved(ImageId)` | Deletes the record |
| `markFailed(ImageId, attempts, nextAttemptAt, reportedAt?)` | Stores the attempt; a non-null `reportedAt` means given up and the record is never returned again |
| `findActive(Collection<ImageId>)` | The subset that has an unreported record |
| `clear(ImageId)` | Deletes the record (used by the sweep) |

## `PendingImagePromotionRepository` (new)

| Operation | Contract |
|---|---|
| `enqueue(OwnedImage, now)` | Adds a due record with `createdAt = now`; ignored if present |
| `takeDue(now, limit, lease)` | Same semantics as removal; each `PendingPromotion` carries `OwnedImage`, `attempts`, `createdAt` |
| `markPromoted(ImageId)` | Deletes the record. After this, readers see `promoted = true` |
| `markFailed(ImageId, attempts, nextAttemptAt, reportedAt?)` | As removal |
| `remove(ImageId)` | Deletes the record if present; no-op otherwise (used when an image is replaced) |
| `deleteByUserId(UserId)` | Deletes every record of the user (account deletion) |

## `UniqueStringIdGenerator` (shared, unchanged)

`nextId()` returns a UUIDv7 string; successive calls from one generator order by time. The core converts it with `ImageId.of`.

## Transaction expectations (set by the services, honoured by adapters)

- `ConfirmImageUploadService`, `DeleteProfileDataService`, `SaveProfileService`: one transaction each, as class-level `@Transactional`.
- `PromotePendingImagesService`, `RemoveObsoleteImagesService`, `SweepUnreferencedImagesService`: no surrounding transaction; each repository call is its own short transaction; provider calls run outside any transaction.
