# Contract: Profile HTTP API

Base path `/api/v1/profiles`. All endpoints need `Authorization: Bearer <access token>` issued by `identity-access`, except the webhook. The caller is always the token's `sub`; no path or body field can name another user for a write (FR-003, FR-007). Requirements: spec FR-001 to FR-008, FR-015.

## Endpoints

| Method and path | Purpose | Success | Notes |
|---|---|---|---|
| `GET /@me` | The caller's profile | 200 `Profile` | Defaults when none saved (FR-001, FR-002) |
| `GET /{userId}` | Any user's profile | 200 `Profile` | `userId` not a positive integer: 400 `validation_failed`, never 404 |
| `PATCH /@me` | Save display name and/or bio | 204 | Partial: omitted field unchanged; bio `""` clears it; `displayName` `""` or blank is refused (FR-003, FR-004) |
| `POST /avatar/upload-signature` | Upload permission for the avatar | 200 `UploadPermission` | Name kept from the legacy service |
| `POST /banner/upload-signature` | Upload permission for the banner | 200 `UploadPermission` | |
| `POST /uploads/notifications` | Provider webhook, upload finished | 204 | Public path, authenticated by the provider's signature (see `image-storage.md`) |

`PATCH` replaces the legacy `PUT /api/v1/profiles` because saves are now partial (clarification 2026-11-04); the path moves to `/@me` to match `identity-access`. A client that sent both fields keeps working by changing the verb and path.

## Bodies

`Profile` (response):

```json
{ "userId": 123456789, "displayName": "", "bio": "", "avatarUrl": null, "bannerUrl": null, "createdAt": null, "updatedAt": null }
```

`userId` is a number, as in `identity-access` responses. `createdAt` and `updatedAt` are ISO-8601 instants, `null` while no row exists.

`PATCH /@me` (request): `{ "displayName": "Mine", "bio": "Hello" }`; either field may be left out. A body with neither field, or `{}`, succeeds and changes nothing.

`UploadPermission` (response): `{ "uploadUrl": "https://api.cloudinary.com/v1_1/{cloud}/image/upload", "method": "POST", "fields": { ... }, "expiresAt": "2026-11-04T10:15:00Z" }`. The client sends `fields` plus the file as multipart form data to `uploadUrl`. The field set is defined in `image-storage.md`.

## Errors

Same body and code vocabulary as `identity-access` (FR-015):

```json
{ "status": 400, "code": "validation_failed", "message": "The request contains invalid values.", "timestamp": "…", "errors": [ { "field": "displayName", "message": "Display name must be 1 to 32 characters." } ] }
```

| Situation | Status | Code |
|---|---|---|
| Missing, expired or badly signed access token; invalid webhook signature | 401 | `unauthenticated` |
| Rule broken (length, blank, control characters), bad `userId` | 400 | `validation_failed` with `errors[].field` naming the rule |
| Unreadable JSON | 400 | `malformed_request` |
| Webhook for a different user than the signed public id, or a file over its limit | 403 | `forbidden` |
| Wrong HTTP method; unsupported content type | 405; 415 | `method_not_allowed`; `unsupported_media_type` |
| Provider unreachable while issuing a permission | 503 | `service_unavailable` |
| Anything unexpected | 500 | `internal_error` |

`not_found` is never returned for a profile read. Field messages name the rule and the real limits and never echo the submitted value.

## Not part of the API

There is no endpoint to create or delete a profile, to read by username, or to change another user's data. Account deletion arrives only through the Kafka notice (`account-deleted-event.md`).
