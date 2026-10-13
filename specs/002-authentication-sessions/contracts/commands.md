# Public Use-Case Contracts: Authentication and Sessions

**Scope**: Java core contracts under `services/identity-access-next/identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/authentication/`. Each interface follows the existing `UseCaseHandler<Command, Result>` style. This document specifies contracts; it is not an HTTP or cookie API.

## Sign in

| Contract | Shape | Behavior |
|----------|-------|----------|
| `AuthenticateUseCase.Command` | `String email`, `String password`, `DeviceInfo deviceInfo` | Normalize email with the existing `Email` rule; require an `ACTIVE` account and matching password |
| `AuthenticateUseCase.DeviceInfo` | `String label` | Nonblank, at most 200 characters; represents the device in later session views |
| `AuthenticateUseCase.Authenticated` | `String accessToken`, `String refreshToken` | Returned only after exactly one `AuthenticationSession` is recorded |

Absent account, wrong password, inaccessible account, or malformed password presentation produces one generic authentication failure and no credentials or session. The refresh credential is returned once at sign-in and is never returned by a session view.

## Renew access

| Contract | Shape | Behavior |
|----------|-------|----------|
| `RefreshAccessTokenUseCase.Command` | `String refreshToken` | Present the existing opaque credential |
| `RefreshAccessTokenUseCase.Refreshed` | `String accessToken` | Returned only for an unrevoked, unexpired session with an `ACTIVE` owner |

Successful renewal updates `lastUsedAt`, leaves the session and refresh credential unchanged, and returns no refresh credential. Blank, malformed, unknown, expired, revoked, or inactive-account presentations fail without issuing access.

## End the current session

| Contract | Shape | Behavior |
|----------|-------|----------|
| `LogoutCurrentSessionUseCase.Command` | `String refreshToken` | Resolve the presented credential |
| `LogoutCurrentSessionUseCase.Result` | `boolean revoked` | `true` only when a usable session is newly revoked; otherwise `false` |

An unknown, expired, or already revoked credential returns `false`. Repeated sign-out never increments a revocation count or changes another session.

## End a selected session

| Contract | Shape | Behavior |
|----------|-------|----------|
| `LogoutSessionUseCase.Command` | `long userId`, `String sessionId`, `String currentPassword` | The caller's authenticated user ID, one owned session ID, and current password |
| `LogoutSessionUseCase.Result` | `boolean revoked` | `true` when that retained, unrevoked session is newly revoked; otherwise `false` |

Verify an `ACTIVE` account and its current password before mutation. A session owned by another account or absent session returns `false` without disclosing its owner. An expired but retained, unrevoked session may be marked revoked; it was already unable to renew. A bad password fails the operation and changes no session.

## End all other sessions

| Contract | Shape | Behavior |
|----------|-------|----------|
| `LogoutOtherSessionsUseCase.Command` | `long userId`, `String currentSessionId`, `String currentPassword` | The caller's authenticated user ID, usable owned current session, and current password |
| `LogoutOtherSessionsUseCase.Result` | `int revokedCount` | Number of other retained, unrevoked sessions newly revoked |

The current session remains usable. A current session that is unknown, non-owned, expired, or revoked cannot authorize this operation. A bad password changes zero sessions. Repeating a successful request returns `revokedCount = 0`.

## List sessions

| Contract | Shape | Behavior |
|----------|-------|----------|
| `FindSessionsQuery.Command` | `long userId` | Authenticated account ID, supplied by the caller boundary |
| `FindSessionsQuery.SessionView` | `String sessionId`, `long userId`, `String deviceInfo`, `Instant issuedAt`, `Instant expiresAt`, nullable `Instant lastUsedAt`, nullable `Instant revokedAt` | Safe projection of one retained session |
| Query result | `List<SessionView>` | Only the requesting `ACTIVE` account's sessions, ordered by issue time descending and session ID ascending |

The query may include retained expired or revoked sessions. It never exposes a refresh credential or stored digest. The presentation layer must obtain `userId` from the authenticated principal; a free-form user ID from a client is not sufficient authorization.

## Shared failure rules

- Authentication and renewal denials produce a generic credential failure. They never reveal whether an email, account, or session exists.
- Session-management commands check account accessibility and password or session ownership as specified above. Bad passwords and invalid current-session authority fail without mutation.
- Repository, credential-generator, or access-token-issuer failures propagate; no use case reports success when its required work failed.
- Core returns credentials as strings. Later presentation code owns transport, including any secure cookie settings.
