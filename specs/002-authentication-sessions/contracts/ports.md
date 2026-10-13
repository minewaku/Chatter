# Internal Port Contracts: Authentication and Sessions

**Scope**: Plain Java interfaces under `authentication/internal/port/`. These signatures describe responsibilities for core implementation and future adapters; they do not prescribe a datastore or token-signing library. Types named here are the existing `UserId` and `Email`, the new `AuthenticationSession`, and the public `FindSessionsQuery.SessionView`.

## Generation and issuance

| Port | Operation | Contract |
|------|-----------|----------|
| `AuthenticationSessionIdGenerator` | `String generate()` | Return a new canonical UUIDv7 string; never use it as a bearer credential |
| `RefreshTokenGenerator` | `String generate()` | Return a nonblank opaque credential from 32 cryptographically random bytes; do not derive it from user or session IDs |
| `AccessTokenIssuer` | `String issue(UserId userId, Email email)` | Return an access credential for that account and email; JWT signing and claims are adapter details |

Generators must fail rather than return a blank or weak value; the repository rejects the unlikely duplicate. Issuance must never log credentials. The refresh-token generator does not rotate an existing credential.

## AuthenticationSessionRepository

| Operation | Contract |
|-----------|----------|
| `void create(AuthenticationSession session, String rawRefreshToken)` | Atomically create one session and its refresh-credential association; reject duplicate IDs or credential digests; store only a digest, never the raw token |
| `Optional<AuthenticationSession> findByPresentedToken(String rawRefreshToken)` | Resolve a presented credential by its digest; return retained revoked or expired records too so core can apply eligibility rules |
| `Optional<AuthenticationSession> findBySessionId(String sessionId)` | Resolve a retained session by its UUIDv7 identity for ownership checks |
| `boolean touchIfUsable(String sessionId, Instant now)` | Set `lastUsedAt` only if the record is unrevoked and `now < expiresAt`; return whether the update occurred |
| `boolean revokeBySessionId(UserId userId, String sessionId, Instant now)` | Set `revokedAt` once for an owned, retained, unrevoked record; include an expired record; return `true` only on a new change |
| `int revokeOtherSessions(UserId userId, String currentSessionId, Instant now)` | Revoke all other retained, unrevoked sessions for the user, including expired records; exclude the current ID; return the actual number changed |
| `int revokeAllByUserId(UserId userId, Instant now)` | Revoke all retained, unrevoked sessions for the user; return the actual number changed |

`touchIfUsable` and all revoke operations require conditional atomic writes. Repeated revocation returns `false` or zero. They must not overwrite a prior `revokedAt` or allow a stale touch to clear it. Credential lookup must not leak raw values into session models, views, or logs. A failed create or conditional write propagates a failure to the calling use case.

Sign-in prepares both credentials before `create` and returns them only after it succeeds. Renewal prepares the access credential before `touchIfUsable` and returns it only after the touch succeeds. `AccessTokenIssuer.issue` creates a string without publishing it; a discarded string after a failed write grants no access.

`LogoutCurrentSessionService` first resolves and checks the presented session and its `ACTIVE` owner; it returns `false` for an expired or already revoked session, then uses `revokeBySessionId` for the conditional write. Targeted and other-session commands check the caller's password and ownership before using the same repository operations.

## AuthenticationSessionViewRepository

| Operation | Contract |
|-----------|----------|
| `List<FindSessionsQuery.SessionView> findByUserId(UserId userId)` | Return only retained sessions owned by this user, including retained expired/revoked records, ordered by `issuedAt` descending then `sessionId` ascending; exclude refresh credentials and digests |

The query service validates the account's accessibility and passes its authenticated `UserId`; the adapter applies the owner filter as defense in depth.

## Synchronous account-change coordination

`AuthenticationSessionRevocationCoordinator.revokeAllFor(UserId userId)` is an authentication-owned application entry point, not a storage port. Existing `ChangePasswordService`, `SuspendUserAccountService`, and `SoftDeleteUserAccountService` call it only after an actual account state change and before reporting success. The coordinator supplies the current time and calls `revokeAllByUserId`.

The caller must not report a completed change while its previous refresh credentials remain usable. A future production adapter must provide atomic account-save/session-revocation coordination or an equivalent guarantee; `@Transactional` in core is not by itself proof across independent stores. Refresh requests begun after a completed revocation must fail. An already-issued access credential has its own validity window and is not invalidated by this port.
