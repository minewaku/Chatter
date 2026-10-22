# Contract: HTTP surface changes

Every request that stays under its limits behaves exactly as before (FR-023). The changes below are the only ones.

## New error codes

| Code | Status | Message | Extra header |
|------|--------|---------|--------------|
| `too_many_attempts` | 429 | Too many attempts. Try again later. | `Retry-After: <whole seconds, at least 1>` |
| `mail_unavailable` | 503 | The email could not be sent right now. Try again later. | `Retry-After: <whole seconds, at least 1>` |

Both use the existing body: `{ "code": ..., "message": ... }` with no field errors and no echoed input. `service_unavailable` (503) is unchanged and now also carries `Retry-After` when it comes from the limiter.

## Per endpoint

| Endpoint | New responses | Counting rule |
|----------|---------------|---------------|
| `POST /api/v1/auth/authenticate` | `429 too_many_attempts`, `503 service_unavailable` | `sign-in` per normalized email. Counts every parseable attempt, cleared on success. Refuses even a correct password while blocked. Unknown emails get the same `429` |
| `POST /api/v1/auth/register` | `201` body gains `verificationMailSent` | None. If the mail could not be sent the account is still created and the field is `false` |
| `POST /api/v1/auth/verification/resend` | `429 too_many_attempts` (was always `202` for a well-formed address) | `resend-gap` then `resend-hourly` per normalized address, for any well-formed address whether or not it has an account. Mail failure after acceptance is hidden: still `202` |
| `PUT /api/v1/users/@me/password` | `429`, `503 service_unavailable` | `reconfirmation` per user |
| `POST /api/v1/users/@me/close` | `429`, `503 service_unavailable` | `reconfirmation` per user |
| `POST /api/v1/users/@me/email-change` | `429`, `503 service_unavailable`, `503 mail_unavailable` | `reconfirmation` per user, then `email-change-request` per user once the password is accepted and the address is free |
| `POST /api/v1/sessions/{sessionId}/revoke` and `POST /api/v1/sessions/revoke-others` (the two session commands that confirm a password) | `429`, `503 service_unavailable` | `reconfirmation` per user |
| Any signed-in endpoint | `503 service_unavailable` returns quickly while the store is held off (was: after the full timeout) | Revocation lookup inside the `sessionStore` breaker |

The two session endpoints are `SessionController` lines 81 and 105. Their commands, `LogoutSessionService` and `LogoutOtherSessionsService`, both call `AccountVerifier`, and `SessionController` catches `AuthenticationFailedException` and rethrows `ReauthenticationFailedException`. `TooManyAttemptsException` and `DependencyUnavailableException` are not `AuthenticationFailedException`, so they pass through that catch untouched.

## Response shapes

`201` from register:

```json
{ "userId": 123, "verificationMailSent": true }
```

`verificationMailSent` is `false` when the account was created but the verification mail could not be sent now. The person can use resend. Older clients that read only `userId` keep working.

`429`:

```
HTTP/1.1 429 Too Many Requests
Retry-After: 540
Content-Type: application/json

{ "code": "too_many_attempts", "message": "Too many attempts. Try again later." }
```

`503 mail_unavailable` (email change only):

```
HTTP/1.1 503 Service Unavailable
Retry-After: 30
Content-Type: application/json

{ "code": "mail_unavailable", "message": "The email could not be sent right now. Try again later." }
```

## Rules that must hold

- A refused or throttled request never ends a session and never reveals whether an account exists (identical shape and status for known and unknown subjects).
- A request that fails body validation (`400`) is never counted.
- A `429` or `503` from this service has the same body shape as the same codes from the gateway, so clients handle them once. The gateway's own limits are separate and unchanged.
- OpenAPI: each endpoint above lists the new codes in `@DocumentedErrors`; the register response schema lists the new field.

## Not changed

- Status codes, bodies, and headers of every other response from specs 001 to 006.
- `GET /api/v1/auth/verification/confirm` and `GET /api/v1/users/email-change/confirm`: untouched (token redemption is not an attempt limit target in this feature).
- Public/protected endpoint allow-list.
