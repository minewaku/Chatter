# Contract: Error Answers

Status and `code` are the wire values from `ErrorCode`; the body is the existing `ApiError` shape with a fixed message. Only the rows marked **changed** differ from today.

## Handler mapping (`ApiExceptionHandler`)

| Core exception | Status | Code | Change |
|---|---|---|---|
| `AuthenticationFailedException` | `401` | `authentication_failed` | none; now thrown only by sign-in and renewal |
| `InvalidCredentialsException` | `403` | `reauthentication_failed` | none |
| `InvalidAccountStateTransitionException` | `403` | `account_inaccessible` | none |
| `UserAccountNotFoundException` | `403` | `account_inaccessible` | **new** |
| `SessionUnusableException` | `401` | `session_unusable` | **new**, new `ErrorCode` |
| `ReauthenticationFailedException` (presentation) | | | **removed** |
| `AccountInaccessibleException` (presentation) | | | **removed** |

`session_unusable` message: "The session is no longer usable."

## Endpoint answers

| Endpoint | Wrong password (incl. fails policy) | Account not ACTIVE | Account row missing | Current session unusable |
|---|---|---|---|---|
| `GET /api/v1/sessions` | n/a | `403 account_inaccessible` (same) | `403 account_inaccessible` (same) | n/a |
| `POST /api/v1/sessions/{sessionId}/revoke` | `403 reauthentication_failed` (same) | `403 account_inaccessible` (**changed** from `reauthentication_failed`) | `403 account_inaccessible` (**changed**) | n/a |
| `POST /api/v1/sessions/revoke-others` | `403 reauthentication_failed` (same) | `403 account_inaccessible` (**changed**) | `403 account_inaccessible` (**changed**) | `401 session_unusable` (**changed** from `403 reauthentication_failed`) |
| `PUT /api/v1/users/@me/password` | `403 reauthentication_failed`, **including a current password that fails the policy** (was `400 validation_failed`) | `403 account_inaccessible` (same) | unchanged | n/a |
| `POST /api/v1/users/@me/email-change` | `403 reauthentication_failed` (same) | `403 account_inaccessible` (same) | unchanged | n/a |
| `POST /api/v1/users/@me/close` | `403 reauthentication_failed` (same) | not applicable, any status may close | unchanged | n/a |
| Sign-in, renewal | `401 authentication_failed` for every cause | same | same | same |

For every row with a password: if the account is not accessible the answer is `account_inaccessible` whatever password was sent, and the password is not compared. The limit answer (`429 too_many_attempts`) is produced before any of these when the limit is reached.

## Documented errors (`@DocumentedErrors`)

| Endpoint | Add |
|---|---|
| `POST /api/v1/sessions/{sessionId}/revoke` | `ACCOUNT_INACCESSIBLE` |
| `POST /api/v1/sessions/revoke-others` | `ACCOUNT_INACCESSIBLE`, `SESSION_UNUSABLE` |
| `GET /api/v1/sessions` | none (already lists `ACCOUNT_INACCESSIBLE`) |
| `PUT /api/v1/users/@me/password`, `POST /api/v1/users/@me/email-change`, `POST /api/v1/users/@me/close` | none (already list `REAUTHENTICATION_FAILED`; password and email-change also `ACCOUNT_INACCESSIBLE`) |

## Accepted nuance

`AccountVerifier` no longer catches `IllegalArgumentException` from `new UserId(userId)`. A non-positive id would now reach the generic `400 validation_failed` handler instead of the old generic denial. The id comes from the validated token subject through `AccessPrincipal.from`, so this is not reachable with a token this service issued; `FindSessionsService` already behaved this way.
