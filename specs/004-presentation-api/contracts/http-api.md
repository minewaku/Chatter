# HTTP API Contract: Identity Access

Base path `/api/v1`. All bodies are `application/json; charset=UTF-8`. Timestamps are ISO-8601 UTC. Protected endpoints require `Authorization: Bearer <accessToken>`. Every non-success response uses the `ApiError` body in [error-model.md](error-model.md). Shapes are defined in [data-model.md](../data-model.md).

## Summary

| # | Method and path | Access | Success | Core use case |
|---|-----------------|--------|---------|---------------|
| 1 | `POST /auth/register` | public | `201` `{userId}` | `RegisterUserUseCase` |
| 2 | `POST /auth/authenticate` | public | `200` token pair | `AuthenticateUseCase` |
| 3 | `POST /auth/verification/resend` | public | `202` no body | `RequestEmailVerificationUseCase` |
| 4 | `GET /auth/verification/confirm?token=` | public | `200` `{status}` | `VerifyEmailUseCase` |
| 5 | `POST /auth/refresh` | public | `200` token pair | `RefreshAccessTokenUseCase` |
| 6 | `POST /auth/logout` | public | `204` no body | `LogoutCurrentSessionUseCase` |
| 7 | `GET /sessions` | protected | `200` list | `FindSessionsQuery` |
| 8 | `POST /sessions/{sessionId}/revoke` | protected | `204` no body | `LogoutSessionUseCase` |
| 9 | `POST /sessions/revoke-others` | protected | `200` `{revokedCount}` | `LogoutOtherSessionsUseCase` |
| 10 | `PUT /users/@me/password` | protected | `204` no body | `ChangePasswordUseCase` |
| 11 | `DELETE /users/@me` | protected | `204` no body | `SoftDeleteUserAccountUseCase` |

The public set (rows 1-6) plus `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**` equals the `identity-access.security.public-endpoints` allow-list in `application.yml`. Public endpoints ignore any `Authorization` header (FR-035).

## 1. Register

Request: `{"email","username","birthday","password"}`.

| Core outcome | Status | Body |
|--------------|--------|------|
| `Registered(userId)` | `201` | `{"userId": 123}` |
| `AccountAlreadyExists` or `DuplicateUserAccountException` | `409` | `account_already_exists` |
| `VerificationPending` | `409` | `verification_pending` |
| invalid request or value | `400` | `validation_failed` |

No password, hash, or token in any response. Registration requests the verification email inside the core use case.

## 2. Authenticate

Request: `{"email","password"}`. The device label comes from the `User-Agent` header (R6 in research).

| Core outcome | Status | Body |
|--------------|--------|------|
| `Authenticated(access, refresh)` | `200` | `{"accessToken":"...","refreshToken":"..."}` |
| `AuthenticationFailedException` (unknown email, wrong password, inactive account, malformed input) | `401` | `authentication_failed`, one identical response for all causes |

No cookie, no credential in a header or URL. Response header `Cache-Control: no-store`.

## 3. Request a verification email

Request: `{"email"}`. Always `202` with no body for a well-formed address, whether or not an eligible account exists. A malformed address is `400 validation_failed`.

## 4. Confirm verification

Query: `token`. Used by the emailed link.

| Core outcome | Status | Body |
|--------------|--------|------|
| `Verified` | `200` | `{"status":"verified"}` |
| `AlreadyVerified` | `200` | `{"status":"already_verified"}` |
| `InvalidOrExpired`, or missing/blank `token` | `400` | `invalid_or_expired_token` |

The token is in the query string, so access logging of the query string is disabled for this path (see quickstart check). Response header `Cache-Control: no-store`.

## 5. Refresh

Request: `{"refreshToken"}`.

| Core outcome | Status | Body |
|--------------|--------|------|
| `Refreshed(access, refresh)` | `200` | `{"accessToken":"...","refreshToken":"..."}`; the presented refresh credential is now dead; the session expiry moved forward |
| `AuthenticationFailedException` (absent, blank, unknown, expired, revoked, already-exchanged credential, inactive account) | `401` | `authentication_failed` |

Response header `Cache-Control: no-store`.

## 6. Logout

Request: `{"refreshToken"}`. Always `204`, whether or not a session was revoked (absent, blank, unknown, expired, or already revoked credentials revoke nothing). When a session is newly revoked, a revocation entry for it is written atomically (FR-030).

## 7. List sessions

Account comes from the access credential. Response: JSON array of `SessionResponse`, newest first then by session id, as the core query orders them. Never contains a refresh credential or digest. An inaccessible account gets `403 account_inaccessible` (edge case).

## 8. End one session

Path `sessionId`; request `{"currentPassword"}`.

| Core outcome | Status | Body |
|--------------|--------|------|
| `Result(revoked=true)` | `204` | none |
| `Result(revoked=false)` (absent, other account's, already revoked) | `404` | `session_not_found`, no ownership hint |
| `AuthenticationFailedException` (wrong password, inaccessible account) | `403` | `reauthentication_failed` |

Ending the session that issued the presented access credential is allowed; the credential stops working from the next request.

## 9. End other sessions

Request `{"currentPassword"}`. The current session is the `sid` of the presented access credential.

| Core outcome | Status | Body |
|--------------|--------|------|
| `Result(revokedCount=n)` | `200` | `{"revokedCount": n}` (0 when repeated) |
| `AuthenticationFailedException` (wrong password, inaccessible account, current session unusable) | `403` | `reauthentication_failed` |

Every revoked session gets a revocation entry in the same step; the current session is untouched.

## 10. Change password

Request `{"currentPassword","newPassword"}`.

| Core outcome | Status | Body |
|--------------|--------|------|
| `Result(changed=true)` | `204` | none; all of the account's sessions are revoked, including the caller's, so the client signs in again |
| `Result(changed=false)` | `409` | `password_unchanged` |
| `InvalidCredentialsException` (wrong current password) | `403` | `reauthentication_failed` |
| `InvalidAccountStateTransitionException` | `403` | `account_inaccessible` |
| `IllegalArgumentException` from the password rule | `400` | `validation_failed` |

## 11. Close own account

No body. Target is always the access credential's `sub`. `Result(changed=true|false)` both give `204` (idempotent). All of the account's sessions are revoked by core, so the access credential stops working from the next request.

## Cross-cutting rules

- `Content-Type` other than JSON on a body endpoint: `415 unsupported_media_type`.
- Unknown path: `404 not_found` in the standard shape; wrong method: `405 method_not_allowed`.
- Field length limits (data-model) bound request size per field, which protects the password hasher; no separate body-size mechanism is added by this feature.
- No request or response body is logged by any component.
