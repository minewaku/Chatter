# Contract: Image Storage Port and Cloudinary Adapter

Core port: `profile.internal.port.ImageStorage`. Only adapter: `CloudinaryImageStorage` in `infrastructure.adapter.outbound.image.storage.cloudinary`. Requirements: spec FR-007 to FR-010. Decisions: research R4 to R7.

## Port

| Method | Returns | Behaviour |
|---|---|---|
| `permit(userId, slot, uploadId, now)` | `UploadPermission` | Signs the upload parameters below. Throws `ImageStorageUnavailableException` only if signing itself fails (it does not call the provider). |
| `verify(rawBody bytes, signature, timestamp, now)` | `Optional<ConfirmedUpload>` | Empty when the signature or its timestamp is invalid or the body is not an upload notification. Otherwise the user, slot, upload id, public id, URL, format and byte size taken from the signed notification. |
| `remove(publicId)` | nothing | Deletes the file. "Not found" is success. Any other failure throws `ImageStorageUnavailableException`. |
| `list(cursor)` | `StoredImagePage` | One page of files under `chatter/profile/` with public id and creation time, plus the next cursor. Throws `ImageStorageUnavailableException`, or a rate-limit variant the sweep treats as "stop and resume next hour". |

No method exposes a Cloudinary type. The adapter uses the Cloudinary Java SDK (`cloudinary-http5`; the legacy service used `cloudinary-http44`) for signing, `destroy` and the Admin API listing.

## Upload permission (what `permit` signs)

| Field | Value |
|---|---|
| `public_id` | `chatter/profile/{avatars\|banners}/{userId}/{uploadId}`, `uploadId` a UUIDv7 |
| `allowed_formats` | `jpg,png,webp` |
| `context` | `userId={userId}\|slot={AVATAR\|BANNER}` |
| `timestamp` | seconds since epoch, from the injected clock |
| `api_key`, `signature` | key id and the SDK's signature over the signed fields |
| `notification_url` | the service's webhook URL, from configuration |

The response also returns `uploadUrl`, `method` (`POST`) and `expiresAt` (`now` plus the configured lifetime, default 15 minutes). The provider's own acceptance window is the hard ceiling (research R4). A size limit is added to the signed fields only if the provider supports one for signed uploads; the confirmation check below does not depend on it.

## Webhook confirmation (what `verify` checks)

| Check | Rule |
|---|---|
| Signature | `X-Cld-Signature` equals the hash of the raw body, the `X-Cld-Timestamp` value and the account's API secret, compared in constant time. Algorithm (SHA-1 or SHA-256) is configuration. |
| Freshness | `X-Cld-Timestamp` is within the configured tolerance (default 2 hours) of `now`. |
| Shape | `notification_type` is `upload`; `public_id` matches the layout above; the `context` custom values `userId` and `slot` agree with the public id. |

`verify` returns the values from the body only after all three pass. The use case then enforces the file limits (format in the allowed list, `bytes` at most the slot limit) and the user rule (research R5). A signature failure answers 401; a limit or user mismatch answers 403; both leave the profile unchanged.

## Removal and listing

- `remove` calls `destroy` for the public id with resource type `image`; a `not found` result is success.
- `list` calls the Admin API resource listing with prefix `chatter/profile/`, the provider's maximum page size, and the cursor from the previous page.

## Configuration

| Property | Meaning | Default |
|---|---|---|
| `profile-next.images.permission-lifetime` | `expiresAt` offset | `PT15M` |
| `profile-next.images.webhook-tolerance` | allowed timestamp skew | `PT2H` |
| `profile-next.images.max-bytes.avatar` / `.banner` | slot size limits | `5MB` / `10MB` |
| `profile-next.images.allowed-formats` | accepted formats | `jpg,png,webp` |
| `profile-next.images.removal.max-retries` | retries before reporting | `10` |
| `profile-next.images.removal.delays` | delay before each retry | `2m,4m,8m,16m,32m,1h` (last repeats) |
| `profile-next.images.removal.poll-interval` | worker period | `PT30S` |
| `profile-next.images.removal.lease` | how long a taken row is hidden from other workers | `PT5M` |
| `profile-next.images.sweep.grace-period` | minimum age before a file is swept | `PT24H` |
| `profile-next.images.sweep.interval` | sweep period | `PT1H` |

Credentials (`cloud-name`, `api-key`, `api-secret`) come from Vault `secret/profilenext` as `cloudinary.cloud-name`, `cloudinary.api-key`, `cloudinary.api-secret`, never from committed files, and are never logged.
