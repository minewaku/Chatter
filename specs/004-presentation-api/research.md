# Research: Identity Access Presentation API

Every Technical Context item was resolvable from the existing code and the spec, so there are no open `NEEDS CLARIFICATION` items. Each entry is Decision / Rationale / Alternatives.

## R1. Refresh and sign-out live under `/api/v1/auth`

- **Decision**: `POST /api/v1/auth/refresh` and `POST /api/v1/auth/logout`, not `/sessions/...` as the old service had.
- **Rationale**: Spec 003's allow-list (`SecurityProperties.DEFAULT_PUBLIC_ENDPOINTS` and `application.yml`) already makes exactly `/api/v*/auth/refresh` and `/api/v*/auth/logout` public. Spec 004 FR-003 requires the public set to equal that allow-list. Both operations are credential exchanges, which fits `auth`. `/sessions` then holds only protected resource operations.
- **Alternatives**: Keep `/sessions/refresh` and edit the allow-list. Rejected: it changes a configured, tested contract for no gain. The spec text was updated to the `/auth` paths.

## R2. Module boundary: presentation never imports infrastructure

- **Decision**: Presentation depends only on core. The security chain in infrastructure obtains its 401/403/503 behavior from `AuthenticationEntryPoint` and `AccessDeniedHandler` beans through `ObjectProvider`, and presentation supplies those beans. The `start` module wires both together. When presentation is absent (infrastructure tests), the chain falls back to Spring's defaults.
- **Rationale**: Spec 003 FR-024 says HTTP errors belong to presentation, but the filter chain that produces some of them lives in infrastructure. Spring's own interfaces let each side stay unaware of the other.
- **Alternatives**: (a) Move the whole security chain into presentation. Rejected: it was just delivered and tested in infrastructure, and key loading is shared with the token issuer. (b) Infrastructure depends on presentation for `ApiError`. Rejected: inverts the dependency direction.

## R3. Core exceptions the API must map move to core `api` packages

- **Decision**: Move `AuthenticationFailedException` to `authentication.api.exception` and `InvalidCredentialsException`, `DuplicateUserAccountException`, `InvalidAccountStateTransitionException` to `user.api.exception`. Behavior unchanged.
- **Rationale**: They are the failure half of the public use-case contracts, but sit in `internal` packages that presentation must not import (FR-026). Moving them is mechanical and keeps one canonical type per failure.
- **Alternatives**: Map by exception class name or message text. Rejected: brittle and unsafe (messages can change). Make presentation import `internal`. Rejected: defeats the structure rule and the ArchUnit check.

## R4. Wrong password on session endpoints is `403`, not `401`

- **Decision**: `AccountVerifier` throws the same generic `AuthenticationFailedException` for wrong password, inaccessible account, and unusable current session. On the three protected endpoints that call it (end one session, end others, change password via `InvalidCredentialsException`), the controller translates it to a presentation `ReauthenticationFailedException` that maps to `403` with code `reauthentication_failed`. On the public endpoints (sign-in, refresh) the same core exception maps to `401` with code `authentication_failed`.
- **Rationale**: Spec FR-018/FR-019 forbids `401` for a wrong current password, because clients react to `401` by refreshing or signing out. Core deliberately conflates the causes so it cannot leak which one applied; presentation keeps that by using one code for all of them on those endpoints.
- **Alternatives**: Change core to distinguish wrong password. Rejected: widens an information leak and changes 002's contract for a presentation concern. Always `401`. Rejected: breaks client refresh logic.

## R5. One error shape, explicit mappings, no catch-all message echo

- **Decision**: A `@RestControllerAdvice` extending `ResponseEntityExceptionHandler` returns `ApiError {status, code, message, timestamp, errors?}` for every failure. Explicit handlers cover: bean-validation (`400 validation_failed` with `errors[{field,message}]`), unreadable body (`400 malformed_request`), core `IllegalArgumentException` from value objects (`400 validation_failed`, fixed generic message, no core message), the moved core exceptions, wrong media type (`415`), wrong method (`405`), and a last-resort `Exception` handler (`500 internal_error`, generic message, logs class name and a correlation id only). The existing catch-all `ApiExceptionHandler` that echoed `e.getMessage()` is deleted.
- **Rationale**: FR-017, FR-018, FR-020. Core value-object messages are written for developers and may one day include a value, so they are never passed through.
- **Alternatives**: RFC 9457 `ProblemDetail` as the body. Not chosen: it is a good fit but the spec fixes the field names (`status`, `code`, `message`, `timestamp`, `errors`). It can be adopted later without changing codes.

## R6. Device label from `User-Agent` with YAUAA, limited fields

- **Decision**: `DeviceLabelResolver` wraps a `UserAgentAnalyzer` built once with `withField` for `DeviceClass`, `OperatingSystemName`, `AgentName`, a small cache, and `hideMatcherLoadStats`. Label: `"<AgentName> on <OperatingSystemName> (<DeviceClass>)"`, dropping unknown parts, falling back to `Unknown device`, truncated to 200 characters. Version 8.2.0.
- **Rationale**: The old service used YAUAA (8.1.1) and the user pointed to it as the reference. Restricting fields cuts start-up time and memory sharply. A hand-rolled regex would mislabel many agents.
- **Alternatives**: Store the raw `User-Agent` (up to 200 chars). Rejected: unreadable in a session list and may carry fingerprinting detail. No parsing, constant label. Rejected: sessions list would be useless.

## R7. OpenAPI with springdoc 3.1.1

- **Decision**: `springdoc-openapi-starter-webmvc-ui` 3.1.1 (the line built for Spring Boot 4; the old service's 2.8.x targets Boot 3). The allow-list already contains `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`. Controllers use `@Operation`, `@ApiResponse`, and schema annotations on request/response records so error shapes appear in the document.
- **Rationale**: FR-028. A start-up test fetches `/v3/api-docs` and asserts all 11 operations exist, which also catches an incompatible springdoc version early.
- **Alternatives**: Hand-written OpenAPI file. Rejected: drifts from code.

## R8. Refresh rotation is one atomic Redis script

- **Decision**: New `session-rotate.lua`. Inputs: session hash key, old token-digest key, new token-digest key, old and new digests, now, new `expiresAt`, retention. In one step it verifies the session exists, is unrevoked, is unexpired, and the old token key maps to this session; then deletes the old token key, creates the new one with a matching expiry, sets `expiresAt` and `lastUsedAt`, and extends the key lifetimes exactly as `session-create.lua` does. Returns `0/1`. Port: `boolean rotate(String sessionId, String presentedToken, String newToken, Instant now, Instant newExpiresAt)`, replacing `touchIfUsable` (no other caller; its script and tests are removed).
- **Rationale**: FR-029 and the two-tab race: the second caller finds the old token key gone and gets the generic failure. A read-then-write sequence in Java would let both succeed.
- **Session age**: No absolute cap (clarified 2026-10-17). `newExpiresAt = now + session lifetime` on every rotation; no maximum age is derived from `issuedAt`.
- **Alternatives**: Keep the old token valid for a grace period. Rejected for now: adds replay-window complexity the spec excludes. Revoke the whole session when an old token reappears. Rejected: spec FR-029 puts reuse detection out of scope.

## R9. Revocation entries: per session, short-lived, written by the revoke scripts

- **Decision**: Key `<prefix>access-revoked:<userId>:<sessionId>`, value the revocation instant, expiry equal to `identity-access.security.jwt.access-token-ttl` from the moment of revocation. `session-revoke-one.lua` and `session-revoke-many.lua` receive that TTL and the key prefix and `SET ... PX` an entry for every session they newly revoke. `revokeAllByUserId` already calls the same script with an empty keep-id, so password change, suspension, and closure are covered without a second mechanism.
- **Rationale**: A token's life is at most TTL after issue, and every token is issued before the revocation instant, so TTL after revocation is the longest a revoked token can still be valid. Permanent storage adds nothing (the session record already holds `revokedAt`) and grows without bound. The account id in the key keeps the idea of an account-scoped key (operators can find or clear one account's entries) without making one device's sign-out log out the others. Atomic with the revocation, so no path (FR-031) can forget it.
- **Alternatives**: (a) Key by account only, as first suggested. Rejected: logs out every device. (b) Account-level cut-off timestamp for revoke-all, plus session entries. Rejected: second mechanism with no benefit now that revoke-all revokes sessions individually. (c) Check the session record on every request. Rejected: reads a hash per request instead of two missing keys, and makes the session store the hot path. (d) Permanent list. Rejected as above.

## R10. Revocation check is a filter, not a token validator

- **Decision**: `RevokedAccessTokenFilter` (a `OncePerRequestFilter`) is added after `BearerTokenAuthenticationFilter`. When the request is authenticated with a `Jwt`, it reads `sub` and `sid`, does one `MGET`-style lookup, and: missing `sid` or a hit means `401` through the entry point; any Redis failure means `503` through the entry point with an `AuthenticationServiceException`. Presentation's entry point maps `AuthenticationServiceException` to `503 service_unavailable` and any other `AuthenticationException` to `401 unauthenticated`.
- **Rationale**: A validator that throws a runtime exception surfaces as a `500` from inside the framework filter and cannot choose `503`. A filter owns the response. FR-033 requires fail-closed `503`.
- **Alternatives**: `OAuth2TokenValidator<Jwt>`. Rejected for the status-code reason. A `HandlerInterceptor` in presentation. Rejected: runs after authorization and would put a Redis dependency in presentation.

## R11. Public endpoints ignore `Authorization`

- **Decision**: `PublicEndpointBearerTokenResolver` wraps `DefaultBearerTokenResolver` and returns `null` for requests matching the configured public endpoints, so Spring never tries to validate a bearer token there.
- **Rationale**: By default `BearerTokenAuthenticationFilter` rejects an invalid or expired token with `401` even on `permitAll` paths. The common case is a client sending its just-expired access token to the refresh endpoint, which must work (FR-035).
- **Alternatives**: Tell clients not to send it. Rejected: fragile and not testable on the server.

## R12. `sid` claim

- **Decision**: `AccessTokenIssuer.issue(UserId, Email, String sessionId)` adds claim `sid`. Both sign-in and refresh pass the session id they already hold. Access tokens without `sid` are refused by the filter.
- **Rationale**: `sid` is the registered OpenID Connect name for a session identifier. It gives revocation its key (FR-030) and gives "end other sessions" its current session without a client-supplied value (FR-014).
- **Alternatives**: A custom claim name. Rejected: no reason to differ from the standard.

## R13. Testing layers

- **Decision**: (1) MockMvc slice tests per controller with use cases mocked, asserting status, body, and absence of sentinels. (2) A shared error-contract test that triggers every code. (3) ArchUnit rules in presentation. (4) Extended Redis contract tests for rotation and revocation entries (infrastructure already has Testcontainers support). (5) An end-to-end suite in `identityaccess-start` using Testcontainers (`postgres:17`, `redis:7`) and a recording mail sender, because only `start` sees all three modules. (6) Spotless `check` in the build.
- **Rationale**: The slice tests are fast and cover mapping. Only the end-to-end suite can prove SC-005, SC-008, and SC-010 across real stores.
- **Alternatives**: Put the end-to-end suite in infrastructure (it has an `e2e` package). Rejected: it cannot see presentation controllers without a dependency the plan forbids.

## R14. Removing the key files and stubs

- **Decision**: Nothing in code or configuration references `presentation/user/.local/jwt-*.pem`. They are untracked (git-ignored by `*.pem`) development files, so the implementation leaves them alone and asks the user before removing them; the key setup is documented only through the quickstart's `services/identity-access-next/.local/` directory. The `presentation/user/` stub package is deleted once its content has moved into the capability packages.
- **Rationale**: FR-025 forbids relying on the files, not touching them. They are the user's local material, so deleting them is the user's call.
- **Alternatives**: Delete them as part of the work. Rejected: destructive to files git cannot restore.
