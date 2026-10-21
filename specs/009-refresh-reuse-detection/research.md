# Research: Refresh Credential Reuse Detection

Facts below were read from the code on 2026-10-21 (`session-rotate.lua`, `session-create.lua`, `session-revoke-one.lua`, `session-revoke-many.lua`, `RedisAuthenticationSessionRepository`, `RefreshAccessTokenService`, `LogoutCurrentSessionService`, `application.yml`). There were no unresolved clarifications in the Technical Context.

## 1. Where the earlier credential is remembered

**Decision**: When `session-rotate.lua` replaces the credential, it no longer deletes the old pointer `session:token:<oldDigest>`. It writes a pointer `session:replaced:<oldDigest>` with the same session id as value, and then deletes the old `session:token:` key as it does today.

**Rationale**: A separate key namespace leaves `findByPresentedToken` and every existing caller untouched. A replaced credential can never be mistaken for a current one because the two lookups read different keys. The data is one small string key per replaced credential, so the existing digest-only storage rule holds (spec FR-009). Writing it in the same script as the rotation makes "replaced" and "current" change together (FR-010).

**Alternatives considered**:
- *Keep the old pointer and change its value to a marker*: one namespace, but every reader of `session:token:` would need to learn the marker, and a bug there could accept a replaced credential. Rejected for the risk.
- *A per-session list or set of earlier digests*: one key per session, but it grows with every renewal and cannot expire entries individually. Rejected.
- *Stateless credentials (session id and counter, authenticated by a server-side MAC)*: O(1) memory per session and no growth, but it changes the credential format and adds a signing secret to manage in Vault. Larger than this feature; recorded as a possible later optimisation, see decision 4.

## 2. When reuse is checked

**Decision**: `RefreshAccessTokenService` calls the new port method `revokeOnReuse(presentedToken, now)` only when `findByPresentedToken` returns nothing. It then refuses with the same `AuthenticationFailedException` either way. If a session is found but is revoked, expired, or its owner is not accessible, no reuse check runs.

**Rationale**: Replaced credentials are never in the current namespace, so "not found" is the only case where a replaced credential can be present. The successful path is unchanged and gets no extra Redis call (spec SC-004 and the performance goal). Skipping the check for revoked or expired sessions satisfies FR-008 without extra script logic, though the script also guards it.

**Alternatives considered**: checking reuse first on every request (extra round trip on the happy path, rejected); doing it in the presentation layer (core would not own a security rule, rejected).

## 3. Concurrent renewals with the same credential

**Decision**: No new code for the race itself. The loser has already found the credential current, then loses in `rotate` (returns false) and throws as today. A request that starts after the winner's script has completed finds nothing in `session:token:` and reaches `revokeOnReuse`.

**Rationale**: This matches the clarified rule (spec FR-013): the dividing line is whether the request began before or after the winning renewal completed, and that is exactly the difference between "found current, lost the rotate" and "not found". The existing test `onlyOneOfManyConcurrentRotationsWithTheSameCredentialSucceeds` already pins one winner; it gains an assertion that the session stays unrevoked.

**Known narrow case**: a request whose lookup reads the pointer in the instant before the winner's script runs and whose own `rotate` runs after is a loser (kept). A request whose lookup reads after the script is reuse (revoked). Both follow the spec; there is no third outcome.

## 4. How long to remember, and what it costs

**Decision** (confirmed 2026-10-22: keep `retention`, no shorter window): A replaced pointer expires `retention` after the replacement (default `P30D`), computed from the application clock the same way the other keys are. This is never later than the session record's own expiry (`newExpiry + retention`), so it satisfies FR-012.

**Rationale and cost**: Defaults are `lifetime P30D`, `retention P30D`, access token `PT15M`. A client that renews every 15 minutes while open replaces its credential up to 96 times a day, so about 2,900 replaced pointers per active session at steady state (30 days of retention). Each is roughly 100 bytes including the key name and overhead, about 290 KB per active such session; 10,000 such sessions would be about 2.9 GB. That is large for a session store.

This is the spec's chosen behavior (FR-003: any earlier credential for as long as the session's records are retained), so the plan follows it, but it is flagged as the main risk. Two ways to reduce it later without changing behavior visibly: a shorter configurable memory window (for example 24 hours: about 96 keys per session), or the stateless-credential design in decision 1. Neither is in scope here; if the cost matters, decide before `/speckit-tasks` and amend FR-003 and the Assumptions.

## 5. How the revoke is made atomic

**Decision**: A new script `session-revoke-reused.lua` takes the `session:replaced:` key, reads the session id, and in one step: confirms the session exists, is not revoked, and is not expired; sets `revokedAt`; writes the same `access-revoked:<userId>:<sessionId>` entry (with the access-token lifetime as TTL) that `session-revoke-one.lua` writes; and returns `"<userId>:<sessionId>"`, or nothing if it changed nothing.

**Rationale**: It reuses the existing revocation contract, so access credentials stop working immediately (FR-002) without any new check in the request path. One script means no state where the session is "revoked" but has no revocation entry (FR-010). It reads the owner from the session hash, so the caller does not need to know the user id, which it does not have for an unknown credential.

**Alternatives considered**: reading the pointer in Java and calling `revokeBySessionId` (two round trips and a window between them, and it needs the user id, rejected).

## 6. Operator-visible record

**Decision**: The Redis adapter logs one `WARN` line when `revokeOnReuse` revokes a session, carrying session id and account id. Core does not log.

**Rationale**: Core has no logging anywhere today; infrastructure and presentation do. The port already puts the access-credential revocation on the adapter as an "adapter obligation", so this follows the same pattern. See [contracts/reuse-event.md](contracts/reuse-event.md).

**Alternatives considered**: a metric counter (useful later, adds a Micrometer dependency to the adapter; not required by the spec); a domain event (no event infrastructure exists for sessions).

## 7. Sign-out and other credential uses

**Decision**: `LogoutCurrentSessionService` is not changed. It keeps calling `findByPresentedToken`, gets nothing for a replaced credential, and returns `revoked = false`.

**Rationale**: Spec clarification: reuse applies to renewal only. This also leaves sign-out idempotent.

## 8. Existing sessions

**Decision**: No migration. Credentials replaced before deployment have no `session:replaced:` key, so reuse of them is refused as unknown (spec Assumptions).
