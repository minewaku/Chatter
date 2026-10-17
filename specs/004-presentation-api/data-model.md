# Data Model: Identity Access Presentation API

The API adds no persistent business entity. It adds HTTP-only shapes, one derived principal, one error shape, and one short-lived Redis entry. All request and response types are immutable records in `identityaccess-presentation`; none is a core type.

## Requests

| Type | Fields (validation) | Maps to core |
|------|---------------------|--------------|
| `RegisterRequest` | `email` (required, email format, max 254), `username` (required, 1-64), `birthday` (required, ISO date, in the past), `password` (required, max 128) | `RegisterUserUseCase.Command` |
| `AuthenticateRequest` | `email` (required), `password` (required) | `AuthenticateUseCase.Command` plus `DeviceInfo(label)` from `DeviceLabelResolver` |
| `ResendVerificationRequest` | `email` (required, email format) | `RequestEmailVerificationUseCase.Command` |
| `RefreshRequest` | `refreshToken` (optional, max 512; absent or blank is treated by core as an invalid credential, giving `401`) | `RefreshAccessTokenUseCase.Command` |
| `LogoutRequest` | `refreshToken` (optional, max 512; absent or blank revokes nothing and still gives `204`) | `LogoutCurrentSessionUseCase.Command` |
| `RevokeSessionRequest` | `currentPassword` (required) | `LogoutSessionUseCase.Command(userId, sessionId, currentPassword)` |
| `RevokeOtherSessionsRequest` | `currentPassword` (required) | `LogoutOtherSessionsUseCase.Command(userId, currentSessionId, currentPassword)` |
| `ChangePasswordRequest` | `currentPassword` (required), `newPassword` (required, max 128) | `ChangePasswordUseCase.Command` |

Validation here is presence, length, and obvious format only. Core value objects (`Email`, `PlainPassword`, username, birthday rules) remain the authority; their `IllegalArgumentException` becomes `400 validation_failed` with a fixed message. The `max` bounds protect the password hasher from oversized input and are not password policy.

Every request record overrides `toString()` to omit secrets so an accidental log call cannot print them. A structure test asserts it.

## Responses

| Type | Fields | Produced for |
|------|--------|--------------|
| `RegisterResponse` | `userId` (number) | `201` registration |
| `TokenPairResponse` | `accessToken`, `refreshToken` | `200` sign-in and `200` refresh |
| `VerificationStatusResponse` | `status`: `verified` or `already_verified` | `200` confirmation |
| `SessionResponse` | `sessionId`, `deviceInfo`, `issuedAt`, `expiresAt`, `lastUsedAt` (nullable), `revokedAt` (nullable) | each element of `200` session list |
| `RevokedCountResponse` | `revokedCount` (integer) | `200` end other sessions |
| `ApiError` | see below | every non-success response |

`SessionResponse` deliberately omits `userId` (present on the core `SessionView`), because the list is already scoped to the caller (FR-012). Timestamps are ISO-8601 UTC instants.

### `ApiError`

| Field | Type | Notes |
|-------|------|-------|
| `status` | number | HTTP status code |
| `code` | string | Stable lowercase identifier from [error-model.md](contracts/error-model.md) |
| `message` | string | Generic, fixed per code; never contains submitted values |
| `timestamp` | instant | ISO-8601 UTC |
| `errors` | list of `{field, message}` | Only for `validation_failed`; `field` is a JSON property name; `message` never contains the submitted value |

## Derived: `AccessPrincipal`

Built from the validated `Jwt` by one helper, never from request input.

| Field | Source | Failure |
|-------|--------|---------|
| `userId` (long) | `sub` | Not a number: `401 unauthenticated` (edge case in spec) |
| `sessionId` (string) | `sid` | Absent or blank: `401 unauthenticated` (tokens issued before this feature) |

Controllers receive it through `@AuthenticationPrincipal Jwt` plus `AccessPrincipal.from(jwt)`.

## Access token claims (changed)

| Claim | Value | Change |
|-------|-------|--------|
| `iss`, `aud`, `iat`, `exp` | configured issuer, audience, issue time, issue time plus `access-token-ttl` | unchanged |
| `sub` | account id | unchanged |
| `email` | account email | unchanged |
| `sid` | session identifier | **new** |

## Revocation entry (new, Redis)

| Part | Value |
|------|-------|
| Key | `identity-access:access-revoked:<userId>:<sessionId>` |
| Value | Revocation instant, epoch microseconds (informational; the key's existence is what matters) |
| Expiry | `access-token-ttl` after the instant of revocation |
| Written by | `session-revoke-one.lua`, `session-revoke-many.lua`, in the same script run that sets `revokedAt` on the session |
| Read by | `RevokedAccessTokenFilter`, one lookup per authenticated request |
| Lifecycle | Created on revoke, expires on its own; never updated, never listed by the application |

An entry is created only for a session that this run newly revoked. A repeated revoke creates nothing and changes no expiry.

## Session record (changed)

The Redis session hash and its token-digest key are unchanged in shape. Renewal now:

| Field / key | Before | After renewal |
|-------------|--------|---------------|
| `expiresAt` | fixed at sign-in | `now + session lifetime` |
| `lastUsedAt` | set by touch | set by rotation |
| refresh token digest key | one digest for the life of the session | old digest key deleted; new digest key points at the same session |
| Hash and digest key lifetimes | session expiry plus retention | recomputed from the new `expiresAt` plus retention |

## State transitions

```text
session:   usable --(refresh)--> usable (new refresh credential, later expiry)
           usable --(logout | revoke one | revoke others | account change)--> revoked  [+ revocation entry]
           usable --(time)--> expired   (no entry needed; refresh refused, access credentials expire by themselves)
entry:     absent --(session revoked)--> present --(access-token-ttl)--> absent
```
