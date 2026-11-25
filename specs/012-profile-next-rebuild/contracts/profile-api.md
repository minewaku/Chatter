# Contract: Profile HTTP API

Base path `/api/v1/profiles`. Every endpoint needs `Authorization: Bearer <access token>` issued by `identity-access`. The caller is always the token's `sub`; no path or body field can name another user for a write (FR-003, FR-007). Requirements: spec FR-001 to FR-008, FR-015.

Updated 2026-11-25 to match the implemented service: images are uploaded with a ticket and then committed by the client. There is no provider webhook and there are no `upload-signature` endpoints any more. `image-storage.md` in this folder still describes the earlier Cloudinary design and is out of date.

## Endpoints

| Method and path | Purpose | Success |
|---|---|---|
| `GET /@me` | The caller's profile; defaults when none was saved (FR-001, FR-002) | 200 `Profile` |
| `GET /{userId}` | Any user's profile; defaults when none was saved | 200 `Profile` |
| `PATCH /@me` | Save display name and/or bio (FR-003, FR-004) | 204 |
| `POST /@me/images/{slot}/upload` | Ask for a ticket to upload one image | 200 `UploadTicket` |
| `POST /@me/images/commit` | Make an uploaded file the profile's image | 200 `CommitResult` |
| `DELETE /@me/images/{slot}` | Empty a slot; also succeeds when it is already empty | 204 |

`{slot}` is `avatar` or `banner`, in lower case. Any other value matches no route.

## Profile

```json
{ "userId": 123456789, "displayName": "", "bio": "", "avatarUrl": null, "bannerUrl": null, "createdAt": null, "updatedAt": null }
```

Every field is always present. `userId` is a number, as in `identity-access` responses. `displayName` and `bio` are `""` when never set. `avatarUrl` and `bannerUrl` are `null` for an empty slot. `createdAt` and `updatedAt` are ISO-8601 instants, `null` while the user has never saved anything.

An image address is `{public-base-url}/profiles/{userId}/{slot}/{imageId}`. A new image always has a new address, so an address can be cached for good. The service does not start without `public-base-url` (`PROFILE_NEXT_PUBLIC_BASE_URL`).

`userId` in `GET /{userId}` must be a positive whole number; anything else is 400 `validation_failed`, never 404.

## Saving text

`PATCH /@me`: `{ "displayName": "Mine", "bio": "Hello" }`. A field that is left out or `null` is unchanged. `{}` succeeds and changes nothing.

| Field | Rule (after trimming) |
|---|---|
| `displayName` | 1 to 32 user-perceived characters; `""` or blank is refused |
| `bio` | At most 190 user-perceived characters; `""` clears it |

Neither may contain control characters (a line break is one) or invisible format characters; the joiners used in emoji are allowed. When both fields are wrong, only the first failure is reported.

## Changing an image

1. Ask for a ticket.
2. Send the file straight to `uploadUrl`. It never passes through this service.
3. Commit the `imageId`.

### 1. Ticket

`POST /@me/images/{slot}/upload`:

```json
{ "contentType": "image/png", "sizeBytes": 48213 }
```

| Slot | Content types | Largest file | Largest width and height |
|---|---|---|---|
| `avatar` | `image/jpeg`, `image/png`, `image/webp` | 5 MiB (5242880 bytes) | 1024 px |
| `banner` | `image/jpeg`, `image/png`, `image/webp` | 10 MiB (10485760 bytes) | 2048 px |

These are the defaults; `profile-next.images.*` can change them.

`UploadTicket` (response):

```json
{
  "imageId": "0197aaab-0000-7000-8000-000000000001",
  "uploadUrl": "https://…",
  "method": "PUT",
  "headers": { "Content-Type": "image/png", "Content-Length": "48213", "if-none-match": "*" },
  "fields": {},
  "expiresAt": "2026-11-25T10:15:00Z"
}
```

A user may hold several tickets at once, also for one slot. A ticket lives 5 minutes by default.

### 2. Upload

Send `method` to `uploadUrl` with every entry of `headers` and the file as the raw body. `fields` is empty for the current storage (Cloudflare R2). The file must be exactly `sizeBytes` long and of the declared type; a browser sets `Content-Length` itself. One ticket can create its file once and cannot overwrite it. The bucket needs a CORS rule for browser uploads (see the object storage runbook).

### 3. Commit

`POST /@me/images/commit`:

```json
{ "imageId": "0197aaab-0000-7000-8000-000000000001" }
```

The ticket knows its slot. The service reads the first bytes of the stored file and judges the format and dimensions from them.

`CommitResult` (response):

```json
{ "slot": "avatar", "outcome": "applied", "imageId": "0197aaab-0000-7000-8000-000000000001", "imageUrl": "https://…/profiles/123456789/avatar/0197aaab-0000-7000-8000-000000000001" }
```

`outcome` is `applied`, or `already_applied` when this image is already the slot's image, so repeating a commit is safe. Committing a ticket retires the caller's other tickets for the same slot.

| Commit answer | Meaning | What the client does |
|---|---|---|
| 409 `upload_incomplete` | The file has not arrived | Finish the upload, then commit again |
| 410 `upload_expired` | The ticket expired | Ask for a new ticket |
| 422 `image_rejected` | The file is not acceptable; it is deleted and the ticket is gone | Choose another file, ask for a new ticket |
| 404 `not_found` | No such ticket for this caller: unknown, malformed, retired, replaced, or long expired | Ask for a new ticket |
| 503 `service_unavailable` | The storage could not be reached; nothing changed | Commit again later |

An expired ticket answers 410 only until the service has cleaned it up (10 minutes after expiry by default), then 404.

## Removing an image

`DELETE /@me/images/{slot}` empties the slot. It does not cancel tickets that are still open: committing one of them afterwards sets that image.

## Errors

Same body shape as `identity-access` (FR-015). `errors` appears only when there are field details.

```json
{ "status": 400, "code": "validation_failed", "message": "The request contains invalid values.", "timestamp": "…", "errors": [ { "field": "displayName", "message": "Display name must be 1 to 32 characters." } ] }
```

| Situation | Status | Code |
|---|---|---|
| Missing, expired or badly signed access token | 401 | `unauthenticated` |
| Text rule broken; bad `userId`; missing `contentType`, `sizeBytes` or `imageId`; `sizeBytes` not positive | 400 | `validation_failed` with `errors[].field` |
| Unreadable body | 400 | `malformed_request` |
| Unknown path or slot; commit of a ticket that does not exist | 404 | `not_found` |
| Wrong HTTP method | 405 | `method_not_allowed` |
| Commit before the file arrived | 409 | `upload_incomplete` |
| Commit after the ticket expired | 410 | `upload_expired` |
| Unsupported content type of the request body | 415 | `unsupported_media_type` |
| Ticket request or file refused by the image rules | 422 | `image_rejected` |
| Anything unexpected | 500 | `internal_error` |
| Storage unreachable or not configured (ticket request, commit) | 503 | `service_unavailable` |

A 422 carries one entry with `field` `image` and one of these messages:

- `The image must be a JPEG, PNG or WebP file.`
- `The file is larger than this image allows.`
- `The image is wider or taller than this image allows.`
- `The file could not be read as an image.`

`not_found` is never returned for a profile read. Field messages never echo the submitted value.

## Not part of the API

There is no endpoint to create or delete a profile, to read by username, or to change another user's data. Account deletion arrives only through the Kafka notice (`account-deleted-event.md`). The service sets no limit on how often tickets are requested; that belongs to the layer in front of it.
