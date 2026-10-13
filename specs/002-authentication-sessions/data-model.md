# Data Model: Authentication and Sessions

**Feature**: `specs/002-authentication-sessions/spec.md`  
**Date**: 2026-10-11

## AuthenticationSession

One bounded, revocable sign-in for one account and one device label. The canonical model name is `AuthenticationSession`.

| Field | Type | Rule |
|-------|------|------|
| `sessionId` | UUIDv7, exposed as a canonical string | Required and globally unique; identifies a record, never used as the refresh proof |
| `userId` | Existing `UserId` | Required; one account owns the session |
| `deviceInfo` | Validated device label | Required; nonblank, at most 200 characters |
| `issuedAt` | `Instant` | Required, captured from injected `Clock` |
| `expiresAt` | `Instant` | Required and strictly after `issuedAt` |
| `lastUsedAt` | Nullable `Instant` | Initially absent; set on successful renewal, never earlier than `issuedAt` |
| `revokedAt` | Nullable `Instant` | Initially absent; set once when a revocation newly succeeds |

The session is usable only when `revokedAt == null`, `now < expiresAt`, and its owning account is `ACTIVE`. Expiry is derived from time; it is not a stored status. Revoked or expired records may remain available to their owner while the retention policy keeps them. Reinstating an account does not clear a session's revocation time.

### State transitions

| Current state | Trigger | Result |
|---------------|---------|--------|
| New | Successful sign-in for an `ACTIVE` account with a matching password | One usable session with no last-use or revocation time |
| Usable | Successful renewal while owner remains `ACTIVE` | Same session and refresh credential; `lastUsedAt` advances; one new access credential |
| Usable | Current, targeted, other-session, or account-change revocation | `revokedAt` set once; no later renewal |
| Usable | Clock reaches `expiresAt` | Expired; no later renewal or current-session revocation success |
| Expired, unrevoked, retained | Targeted, other-session, or account-change revocation | `revokedAt` set once and counted as a newly revoked retained record |
| Expired or revoked | Renewal or current-session sign-out | No new credential or current-session revocation |
| Revoked | Any repeat revocation | No new revocation count |

Conditional persistence must prevent stale renewal updates from clearing a revocation and must return the actual number of newly revoked records.

## Refresh credential association

Each `AuthenticationSession` has one non-rotating refresh credential association. The raw credential is a separate opaque random value with 32 bytes of entropy before encoding; it exists only when issued or presented. The future storage adapter keeps a unique, deterministic digest linked to `sessionId` and resolves presented credentials by deriving that digest. Neither the raw credential nor its digest is a field in `AuthenticationSession` or `SessionView`.

The association ends when the record is no longer retained. Session expiry or revocation makes the credential unusable even if its digest remains stored for a retained session view.

## Existing UserAccount relationship

`AuthenticationSession.userId` refers to the existing user account. `UserAccount.isAccessible()` is the source of truth for eligibility; only `ACTIVE` returns true. `PENDING_VERIFICATION`, `LOCKED`, `SUSPENDED`, and `DELETED` cannot sign in, renew, or manage sessions. Successful password changes, suspensions, and soft deletions revoke all retained sessions for that account. A no-op password change or rejected account transition does not revoke them.

The account's email at issue time is supplied to `AccessTokenIssuer` with its `UserId`. An access credential already issued before revocation is not itself a session record and is not promised immediate invalidation by this feature.

## DeviceInfo and SessionView

`AuthenticateUseCase.DeviceInfo` is a public command value containing one `label` string. Core validates the label and stores it with the session. The query projects it as `SessionView.deviceInfo` without browser-header or network-address fields.

`SessionView` contains `sessionId`, `userId`, `deviceInfo`, `issuedAt`, `expiresAt`, `lastUsedAt`, and `revokedAt`. It is scoped to the authenticated account and can include retained expired or revoked sessions. Results use a deterministic order: `issuedAt` descending, then `sessionId` ascending. At least 100 retained sessions for one account must be representable without exposing another account's records.
