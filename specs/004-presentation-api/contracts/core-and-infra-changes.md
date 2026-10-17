# Contract Changes to Core and Infrastructure

These are the changes below the HTTP layer that spec 004 requires. They amend specs 002 and 003; the matching edits to `specs/002-authentication-sessions/contracts/{commands,ports}.md` and `specs/003-infrastructure-adapters/contracts/adapter-map.md` are tasks, not part of this plan.

## Core: public contracts

| Item | Before | After |
|------|--------|-------|
| `RefreshAccessTokenUseCase.Refreshed` | `Refreshed(String accessToken)` | `Refreshed(String accessToken, String refreshToken)` |
| Renewal behavior | Touch `lastUsedAt`; refresh credential and expiry unchanged | Issue a new refresh credential, replace the stored digest, set `expiresAt = now + session lifetime` and `lastUsedAt = now`, all in one repository call; the presented credential is dead afterwards |
| Failure on a stale or already-exchanged credential | n/a | `AuthenticationFailedException`, session untouched |
| Exceptions | `authentication.internal.exception.AuthenticationFailedException`; `user.internal.exception.{InvalidCredentialsException, DuplicateUserAccountException, InvalidAccountStateTransitionException}` | Same classes in `authentication.api.exception` and `user.api.exception`; no other change |

Contract text in spec 002 (`commands.md` "Renew access") is updated: "Successful renewal ... returns no refresh credential" becomes the rotation behavior above.

## Core: outbound ports

| Port | Change |
|------|--------|
| `AccessTokenIssuer` | `String issue(UserId userId, Email email, String sessionId)`; the adapter puts `sessionId` in the `sid` claim |
| `AuthenticationSessionRepository` | Remove `touchIfUsable`. Add `boolean rotate(String sessionId, String presentedRefreshToken, String newRefreshToken, Instant now, Instant newExpiresAt)`: atomically, only when the session is unrevoked, unexpired, and the presented credential belongs to it, replace the credential digest, set `expiresAt` and `lastUsedAt`, and return `true`; otherwise change nothing and return `false` |
| `AuthenticationSessionRepository` revoke methods | Signatures unchanged. Contract note added: every call that newly revokes a session also makes that session's access credentials unusable for the access-credential lifetime (adapter obligation, FR-030/FR-031) |

## Core: services

| Class | Change |
|-------|--------|
| `AuthenticateService` | Pass `session.sessionId()` to `accessTokens.issue` |
| `RefreshAccessTokenService` | Inject `RefreshTokenGenerator` and the session lifetime; generate the new credential; call `rotate`; on `false` throw `AuthenticationFailedException`; issue the access credential with the session id; return both credentials. Order: verify the owner is accessible, issue the access credential, generate the new refresh credential, then rotate; the access credential is discarded unseen when rotation fails, and an issuer failure leaves the session and presented credential untouched |

Core test fakes (`AuthenticationTestFakes`) gain `rotate`, a revocation-aware revoke, and the `sessionId` argument.

## Infrastructure

| Item | Change |
|------|--------|
| `JwtAccessTokenIssuer` | Adds claim `sid` |
| `session-rotate.lua` (new) | Atomic rotation as in research R8 |
| `session-revoke-one.lua`, `session-revoke-many.lua` | Take the revocation key prefix and `access-token-ttl` (milliseconds); for each session newly revoked, `SET <prefix>access-revoked:<userId>:<sessionId> <nowMillis> PX <ttl>` |
| `RedisAuthenticationSessionRepository` | Calls the new/extended scripts; reads `access-token-ttl` through `JwtProperties`; removes the touch script and method |
| `RevokedAccessTokenFilter` (new) | Per research R10 |
| `PublicEndpointBearerTokenResolver` (new) | Per research R11 |
| `SecurityConfiguration` | Registers the resolver and filter; uses `ObjectProvider<AuthenticationEntryPoint>` and `ObjectProvider<AccessDeniedHandler>` with Spring defaults as fallback |
| `application.yml` (`start`) | No new required keys; `identity-access.security.jwt.access-token-ttl` is now also the revocation-entry lifetime |

## Compatibility

- Access credentials issued before the change have no `sid` and are refused with `401`; users sign in again. No data migration.
- Existing sessions in Redis keep working: rotation reads the same hash and digest keys.
- Existing revoked sessions have no revocation entries. Their access credentials expire within one access-token lifetime after deployment.

## Test obligations

| Area | Required test |
|------|---------------|
| Rotation | Old credential refused after rotation; two concurrent rotations with one credential: exactly one succeeds; rotation of revoked or expired session returns `false` and changes nothing; expiry and key lifetimes move forward |
| Revocation entries | Created for exactly the sessions newly revoked, with the access-token TTL and no longer; repeated revoke creates none; revoke-all and revoke-others create one per session and spare the kept one; entry survives only until TTL |
| Filter | Revoked `sid` refused; other session's token accepted; token without `sid` refused; Redis down gives `503`; public endpoint with an invalid bearer token proceeds |
| Fail-closed edge | Entries flushed: a revoked token works until its own expiry and the session still cannot refresh |
