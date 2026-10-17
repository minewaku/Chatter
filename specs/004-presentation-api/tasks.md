---

description: "Task list for the Identity Access Presentation API"
---

# Tasks: Identity Access Presentation API

**Input**: Design documents from `specs/004-presentation-api/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: Included. The spec's success criteria (SC-002 to SC-010) and the structure rule (FR-026) can only be met with automated tests, so each story writes its tests first and sees them fail before implementing.

**Organization**: Tasks are grouped by user story. Foundational work (Phase 2) blocks every story.

## Format: `- [ ] [TaskID] [P?] [Story?] Description with file path`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1 to US5, matching `spec.md`

## Path Conventions

All paths are relative to `services/identity-access-next/` unless they start with `specs/` or `infrastructure/`. `<P>` stands for `com/minewaku/chatter/identityaccess`. Examples: `identityaccess-core/src/main/java/<P>/authentication/...`, `identityaccess-presentation/src/main/java/<P>/presentation/...`. Run Maven only through `./mvnw` in `services/identity-access-next/` (never a bare `mvn`).

Controller split used below (all under `/api/v1/auth` or `/api/v1/sessions` or `/api/v1/users`, so the HTTP contract in `contracts/http-api.md` is unchanged): `AuthController` (register, authenticate), `VerificationController` (resend, confirm), `TokenController` (refresh, logout), `SessionController`, `UserController`. The split lets stories proceed in parallel without editing the same file.

---

## Phase 1: Setup

- [X] T001 Baseline: from `services/identity-access-next/` run `./mvnw verify` and record that core, infrastructure, presentation, and start build before any change. Stop and report if the baseline is red.
- [X] T002 [P] Update `identityaccess-presentation/pom.xml`: add `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `org.springdoc:springdoc-openapi-starter-webmvc-ui` 3.1.1 (property `springdoc.version`), `nl.basjes.parse.useragent:yauaa` 8.2.0 (property `yauaa.version`); keep `spring-boot-starter-webmvc` and `spring-boot-starter-validation`; add test-scope `spring-boot-starter-test`, `spring-security-test`, and `com.tngtech.archunit:archunit-junit5` with the same `archunit.version` property (1.5.1) used in `identityaccess-infrastructure/pom.xml`. Do not add any dependency on `identityaccess-infrastructure`.
- [X] T003 [P] Update `identityaccess-start/pom.xml`: add test-scope `spring-boot-starter-test`, `spring-security-test`, `org.testcontainers:testcontainers-junit-jupiter`, `org.testcontainers:testcontainers-postgresql`, and `spring-boot-starter-data-redis` is already transitive through infrastructure (verify, add only if missing).

---

## Phase 2: Foundational (blocks all user stories)

**Purpose**: contract changes below the HTTP layer that every story needs, the shared error model, the security wiring, and the test harnesses.

### Core contract changes

- [X] T004 Move `AuthenticationFailedException` from `identityaccess-core/src/main/java/<P>/authentication/internal/exception/` to `identityaccess-core/src/main/java/<P>/authentication/api/exception/` (same class, same behavior, use `git mv`). Update every import found with `grep -rn AuthenticationFailedException` under `identityaccess-core/src`, including `AccountVerifier.java`, `AuthenticateService.java`, `LogoutOtherSessionsService.java`, `RefreshAccessTokenService.java`, `FindSessionsService.java`, and tests `AuthenticateServiceTest.java`, `FindSessionsServiceTest.java`, `LogoutOtherSessionsServiceTest.java`, `LogoutSessionServiceTest.java`, `RefreshAccessTokenServiceTest.java`.
- [X] T005 Move `InvalidCredentialsException`, `DuplicateUserAccountException`, and `InvalidAccountStateTransitionException` from `identityaccess-core/src/main/java/<P>/user/internal/exception/` to `identityaccess-core/src/main/java/<P>/user/api/exception/` (`git mv`, no behavior change). Update imports in `UserAccount.java`, `UserAccountApplicationServiceTest.java`, `UserAccountTestFakes.java`, `UserAccountTest.java`, and in infrastructure `identityaccess-infrastructure/src/main/java/<P>/infrastructure/adapter/outbound/user/write/jdbc/JdbcUserAccountRepository.java` and its test `JdbcUserAccountRepositoryTest.java`. Confirm with `grep -rn "internal.exception" identityaccess-*/src` that no reference remains.
- [X] T006 Change `AccessTokenIssuer` in `identityaccess-core/src/main/java/<P>/authentication/internal/port/AccessTokenIssuer.java` to `String issue(UserId userId, Email email, String sessionId)`. In `AuthenticateService.java` pass `session.sessionId()`. In `RefreshAccessTokenService.java` pass `session.sessionId()` (rotation itself is US2). Update the issuer fake in `identityaccess-core/src/test/java/<P>/authentication/internal/application/AuthenticationTestFakes.java` and extend `AuthenticateServiceTest.java` to assert the issuer received the stored session's identifier.
- [X] T007 [P] In `identityaccess-infrastructure/src/main/java/<P>/infrastructure/adapter/outbound/authentication/JwtAccessTokenIssuer.java` add claim `sid` set to the `sessionId` argument; extend `JwtAccessTokenIssuerTest.java` (same package under `src/test`) to assert `sid` is present and equals the argument, and the other claims (`iss`, `aud`, `sub`, `email`, `iat`, `exp`) are unchanged.
- [X] T008 Run `./mvnw -pl identityaccess-core,identityaccess-infrastructure -am verify` and confirm green before continuing.

### Error model and security wiring

- [X] T009 [P] Create `ApiError` (record: `int status`, `String code`, `String message`, `Instant timestamp`, `List<FieldError> errors` nullable and omitted from JSON when null, nested record `FieldError(String field, String message)`) and `ErrorCode` (enum of the codes in `contracts/error-model.md` with their HTTP status and fixed generic message) in `identityaccess-presentation/src/main/java/<P>/presentation/shared/error/`. Messages are fixed per code and never contain submitted values.
- [X] T010 [P] Create `AccessPrincipal` (record `long userId`, `String sessionId`; static `from(Jwt)` reads `sub` and `sid`; throws `org.springframework.security.oauth2.server.resource.InvalidBearerTokenException` when `sub` is not a number or `sid` is absent or blank) in `identityaccess-presentation/src/main/java/<P>/presentation/shared/security/AccessPrincipal.java`, with `AccessPrincipalTest.java` under `src/test/java/<P>/presentation/shared/security/`.
- [X] T011 Create `ApiExceptionHandler` (`@RestControllerAdvice` extending `ResponseEntityExceptionHandler`) in `identityaccess-presentation/src/main/java/<P>/presentation/shared/error/ApiExceptionHandler.java` with explicit mappings from `contracts/error-model.md`: bean-validation to `validation_failed` with `errors[{field,message}]` using JSON property names and no rejected values; unreadable body to `malformed_request`; core `IllegalArgumentException` to `validation_failed` with the fixed message (never the exception message); `AuthenticationFailedException` to `authentication_failed` (401); `InvalidCredentialsException` to `reauthentication_failed` (403); `InvalidAccountStateTransitionException` to `account_inaccessible` (403); `DuplicateUserAccountException` to `account_already_exists` (409); unsupported media type 415, method 405, no handler 404 in the standard shape; last-resort `Exception` to `internal_error` (500). Log only the code, the exception class name, and a generated correlation id. Delete the old stub `identityaccess-presentation/src/main/java/<P>/presentation/user/ApiExceptionHandler.java`.
- [X] T012 [P] Create `ApiAuthenticationEntryPoint` and `ApiAccessDeniedHandler` in `identityaccess-presentation/src/main/java/<P>/presentation/shared/error/`, both `@Component`, writing the `ApiError` JSON with `ObjectMapper`: entry point returns `401 unauthenticated` with `WWW-Authenticate: Bearer` for any `AuthenticationException`, except `AuthenticationServiceException` which returns `503 service_unavailable` with `Retry-After: 5`; access-denied handler returns `403 forbidden`. Neither logs request data.
- [X] T013 [P] Create `PublicEndpointBearerTokenResolver` in `identityaccess-infrastructure/src/main/java/<P>/infrastructure/security/PublicEndpointBearerTokenResolver.java`: implements `BearerTokenResolver`, wraps `DefaultBearerTokenResolver`, and returns `null` when the request path matches any pattern of `SecurityProperties.publicEndpoints()` (use the same Ant-style matching as `SecurityConfiguration`). Test in `identityaccess-infrastructure/src/test/java/<P>/infrastructure/security/PublicEndpointBearerTokenResolverTest.java`: public path with `Authorization: Bearer garbage` returns null; protected path returns the token.
- [X] T014 Update `identityaccess-infrastructure/src/main/java/<P>/infrastructure/security/SecurityConfiguration.java`: register `PublicEndpointBearerTokenResolver` via `oauth2ResourceServer(o -> o.jwt(...).bearerTokenResolver(...))`; use `ObjectProvider<AuthenticationEntryPoint>` and `ObjectProvider<AccessDeniedHandler>` (`getIfAvailable`) to set `.authenticationEntryPoint(...)` on the resource server and `.exceptionHandling(...)` when presentation provides them, otherwise keep Spring defaults. Extend `SecurityChainTest.java` (same package under `src/test`): an expired bearer token sent to `/api/v1/auth/authenticate` proceeds (no 401), the same token to a protected path returns 401, and a custom entry point bean is used when present.

### Structure checks and end-to-end harness

- [X] T015 [P] Create `PresentationArchitectureTest` in `identityaccess-presentation/src/test/java/<P>/presentation/structure/PresentationArchitectureTest.java` (ArchUnit): classes in `..presentation..` MUST NOT depend on `..infrastructure..`, on `..internal..` packages of core, or on `org.springframework.data..`, `io.lettuce..`, `org.postgresql..`, `jakarta.mail..`; controllers live only in `..presentation.authentication..`, `..presentation.session..`, `..presentation.user..`; request/response/error records live in the same capability package or in `..presentation.shared..` and none is a core type. FR-026, FR-027. The test is expected to pass already; it guards later tasks.
- [X] T016 [P] Create the end-to-end harness in `identityaccess-start/src/test/java/<P>/start/e2e/`: `E2eSupport` (JUnit 5 extension or abstract base starting Testcontainers `postgres:17` and `redis:7` and exposing their URLs as Spring properties via `@DynamicPropertySource`), `RecordingMailSender` (implements `JavaMailSender`, stores messages so tests can extract the verification link and token), `TestKeys` (generates an RSA 2048 key pair at test start and supplies `identity-access.security.jwt.private-key`/`public-key` properties), and `ApiClient` (small helper over `TestRestTemplate`/`WebTestClient` with methods per endpoint returning status plus parsed body). Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` on `StartApplication`. Include one smoke test `StartSmokeE2eTest` that boots the context and gets `/actuator/health` = UP.

**Checkpoint**: core and infrastructure carry the new contracts; presentation has its error model; the harness boots. Stories can start.

---

## Phase 3: User Story 1 - Register, Verify, and Sign In Over HTTP (Priority: P1) MVP

**Goal**: A new person registers, confirms the emailed token, and signs in with a JSON token pair.

**Independent Test**: In the end-to-end suite register, read the link from `RecordingMailSender`, confirm it (twice), sign in, and call a protected endpoint with the access token. Quickstart section 4.

### Tests for User Story 1 (write first, confirm they fail)

- [X] T017 [P] [US1] `DeviceLabelResolverTest` in `identityaccess-presentation/src/test/java/<P>/presentation/authentication/DeviceLabelResolverTest.java`: Chrome-on-Linux UA gives a label like `Chrome on Linux (Desktop)`; null or blank header gives `Unknown device`; unparseable agent gives a nonblank label; a 5,000-character agent string gives a label of at most 200 characters.
- [X] T018 [P] [US1] `AuthControllerTest` (`@WebMvcTest`, mocked `RegisterUserUseCase` and `AuthenticateUseCase`, `@MockitoBean`) in `identityaccess-presentation/src/test/java/<P>/presentation/authentication/AuthControllerTest.java`: register `Registered(7)` gives 201 `{"userId":7}`; `AccountAlreadyExists` gives 409 `account_already_exists`; `VerificationPending` gives 409 `verification_pending`; missing/invalid field gives 400 `validation_failed` with `errors[].field`; authenticate success gives 200 with exactly the fields `accessToken` and `refreshToken`, no `Set-Cookie` header, and `Cache-Control: no-store`; `AuthenticationFailedException` gives 401 `authentication_failed`; the `AuthenticateUseCase.Command` received carries a nonblank device label derived from the `User-Agent` header. No response body contains the submitted password.
- [X] T019 [P] [US1] `VerificationControllerTest` in `identityaccess-presentation/src/test/java/<P>/presentation/authentication/VerificationControllerTest.java`: resend always 202 with empty body for a well-formed email and 400 `validation_failed` for a malformed one; confirm `Verified` gives 200 `{"status":"verified"}`, `AlreadyVerified` gives 200 `{"status":"already_verified"}`, `InvalidOrExpired` and a missing or blank `token` give 400 `invalid_or_expired_token`; confirm responses carry `Cache-Control: no-store`.

### Implementation for User Story 1

- [X] T020 [P] [US1] Create request/response records in `identityaccess-presentation/src/main/java/<P>/presentation/authentication/`: `RegisterRequest` (`email` required, email format, max 254; `username` required, 1-64 characters; `birthday` required, ISO date, in the past; `password` required, max 128), `AuthenticateRequest` (`email` required; `password` required, max 128), `RegisterResponse(long userId)`, `TokenPairResponse(String accessToken, String refreshToken)`. Every request record overrides `toString()` to print only the type name. Move/evolve the stub `identityaccess-presentation/src/main/java/<P>/presentation/user/RegisterRequest.java` into this package and delete the stub.
- [X] T021 [P] [US1] Create `ResendVerificationRequest` (`email` required, email format, max 254) and `VerificationStatusResponse(String status)` in `identityaccess-presentation/src/main/java/<P>/presentation/authentication/`, `toString()` omitting values.
- [X] T022 [US1] Create `DeviceLabelResolver` in `identityaccess-presentation/src/main/java/<P>/presentation/authentication/DeviceLabelResolver.java` backed by one `UserAgentAnalyzer` built once as a `@Bean` (restricted to fields `DeviceClass`, `OperatingSystemName`, `AgentName`, small cache, `hideMatcherLoadStats`): label `"<AgentName> on <OperatingSystemName> (<DeviceClass>)"` omitting unknown parts, falling back to `Unknown device`, truncated to 200 characters. The network address is not used. Makes T017 pass.
- [X] T023 [US1] Create `AuthController` in `identityaccess-presentation/src/main/java/<P>/presentation/authentication/AuthController.java` (`@RequestMapping("/api/v1/auth")`): `POST /register` and `POST /authenticate` per `contracts/http-api.md` sections 1-2, using `@Valid @RequestBody`, mapping core results to the HTTP outcomes (`AccountAlreadyExists` 409 `account_already_exists`, `VerificationPending` 409 `verification_pending`), `Cache-Control: no-store` on authenticate, no cookie, no logging of the request. Delete the old stub `identityaccess-presentation/src/main/java/<P>/presentation/user/AuthController.java`. Makes T018 pass.
- [X] T024 [US1] Create `VerificationController` in `identityaccess-presentation/src/main/java/<P>/presentation/authentication/VerificationController.java`: `POST /api/v1/auth/verification/resend` (202, empty body) and `GET /api/v1/auth/verification/confirm?token=` per `contracts/http-api.md` sections 3-4; the `token` parameter is optional at binding and a missing or blank value is mapped to `invalid_or_expired_token`; `Cache-Control: no-store`; the token is never logged. Makes T019 pass.
- [X] T025 [US1] End-to-end `RegistrationFlowE2eTest` in `identityaccess-start/src/test/java/<P>/start/e2e/RegistrationFlowE2eTest.java`: register (201), register again (409 `verification_pending`), sign in before verification (401 `authentication_failed`), extract the token from `RecordingMailSender`, confirm (200 `verified`), confirm again (200 `already_verified`), sign in (200, two fields, no `Set-Cookie`), call a protected path with the access token (not 401), register the same email again (409 `account_already_exists`). Also assert sign-in with unknown email, wrong password, and not-active account return byte-identical status and body (SC-006), and verification resend returns identical responses for eligible and ineligible emails.

**Checkpoint**: User Story 1 works end to end and is demonstrable (MVP).

---

## Phase 4: User Story 2 - Keep a Session Alive and End It (Priority: P1)

**Goal**: Refresh rotates the refresh credential and renews the session lifetime; sign-out revokes the session and blocks its access credentials immediately without affecting other sessions.

**Independent Test**: Quickstart sections 5 and 6 and the end-to-end test in T038.

### Tests for User Story 2 (write first, confirm they fail)

- [X] T026 [P] [US2] Update `identityaccess-core/src/test/java/<P>/authentication/internal/application/RefreshAccessTokenServiceTest.java`: success returns both credentials and calls `rotate` once with `newExpiresAt = now + session lifetime` (no cap, per clarification); `rotate` returning false throws `AuthenticationFailedException` and no access credential is issued; blank, unknown, expired, revoked sessions and inaccessible owners still fail generically; the access credential is issued with the session id. Update `AuthenticationTestFakes.java` fake repository: remove `touchIfUsable`, add `rotate` with the same conditions as the contract, and make the fake revoke methods record revoked ids.
- [X] T027 [P] [US2] Extend `identityaccess-infrastructure/src/test/java/<P>/infrastructure/adapter/outbound/session/write/redis/RedisAuthenticationSessionRepositoryTest.java` (Testcontainers Redis) with: rotation replaces the credential so the old one is not found and the new one is, `expiresAt` and `lastUsedAt` move forward and key lifetimes are recomputed; rotation of a revoked, expired, unknown, or mismatched-credential session returns false and changes nothing; two concurrent rotations with the same credential give exactly one true; revoking one session creates `identity-access:access-revoked:<userId>:<sessionId>` with a lifetime not above the access-token TTL; a repeated revoke creates no second entry and does not reset the lifetime; revoke-others creates entries for each revoked session and none for the kept one; revoke-all creates one per revoked session; entries disappear after the TTL (use a short configured TTL). Remove the `touchIfUsable` tests.
- [X] T028 [P] [US2] `RevokedAccessTokenFilterTest` in `identityaccess-infrastructure/src/test/java/<P>/infrastructure/security/RevokedAccessTokenFilterTest.java` (MockMvc with embedded Redis test support from `RedisTestSupport`): token whose `<sub>:<sid>` entry exists is refused 401; a token of another session of the same account is accepted; a token without `sid` is refused 401; with Redis stopped a protected request gets 503 and a public request still proceeds; requests with no authentication are untouched.
- [X] T029 [P] [US2] `TokenControllerTest` in `identityaccess-presentation/src/test/java/<P>/presentation/authentication/TokenControllerTest.java`: refresh success gives 200 with exactly `accessToken` and `refreshToken` and `Cache-Control: no-store`; `AuthenticationFailedException` gives 401 `authentication_failed`; a body with no `refreshToken` or a blank one still reaches the use case and gives 401; logout always gives 204 with empty body, for `Result(true)`, `Result(false)`, and a missing or blank token.

### Implementation for User Story 2

- [X] T030 [US2] Core: in `identityaccess-core/src/main/java/<P>/authentication/internal/port/repository/AuthenticationSessionRepository.java` remove `touchIfUsable` and add `boolean rotate(String sessionId, String presentedRefreshToken, String newRefreshToken, Instant now, Instant newExpiresAt)` with the contract Javadoc from `contracts/core-and-infra-changes.md`. Change `RefreshAccessTokenUseCase.Refreshed` to `Refreshed(String accessToken, String refreshToken)`. Rewrite `RefreshAccessTokenService.java` to inject `RefreshTokenGenerator` and the existing session-lifetime `Duration` (same bean `AuthenticateService` uses): verify the owner is accessible, generate the new credential, `rotate` (false means `AuthenticationFailedException`), then issue the access credential with the session id, return both. Makes T026 pass.
- [X] T031 [US2] Infrastructure: create `identityaccess-infrastructure/src/main/resources/redis/session-rotate.lua` implementing the atomic rotation in `research.md` R8 (verify session exists, unrevoked, `now < expiresAt`, old token-digest key maps to this session; delete old token key; create new token key; set `expiresAt` and `lastUsedAt`; recompute key lifetimes exactly as `session-create.lua` does; return 1 or 0). Delete `session-touch.lua`.
- [X] T032 [US2] Infrastructure: extend `session-revoke-one.lua` and `session-revoke-many.lua` in `identityaccess-infrastructure/src/main/resources/redis/` to take the revocation key prefix and the access-token TTL in milliseconds as extra arguments and, for each session they newly revoke, `SET <prefix>access-revoked:<userId>:<sessionId> <nowMillis> PX <ttl>` in the same script run. A session already revoked gets no entry and no TTL reset. Update the header comments of both scripts.
- [X] T033 [US2] Infrastructure: update `RedisAuthenticationSessionRepository.java` (`identityaccess-infrastructure/src/main/java/<P>/infrastructure/adapter/outbound/session/write/redis/`): implement `rotate` with `session-rotate.lua` (convert instants to the same epoch-microsecond/millisecond units the other scripts use), pass the key prefix and `JwtProperties.accessTokenTtl()` to the two revoke scripts, remove `touchIfUsable` and its script constant. Update its construction in `RedisSessionWriterFactory.java` (test sources) and any `@Configuration` that builds it so the access-token TTL is supplied. Makes T027 pass.
- [X] T034 [US2] Infrastructure: create `RevokedAccessTokenFilter` in `identityaccess-infrastructure/src/main/java/<P>/infrastructure/security/RevokedAccessTokenFilter.java` (`OncePerRequestFilter`): when the `SecurityContext` holds a `JwtAuthenticationToken`, read `sub` and `sid`; if `sid` is missing or blank call the `AuthenticationEntryPoint` with `InvalidBearerTokenException`; otherwise do one Redis `EXISTS`/`GET` on `<keyPrefix>access-revoked:<sub>:<sid>`; a present key calls the entry point with `InvalidBearerTokenException`; any `DataAccessException` or Redis failure calls the entry point with `AuthenticationServiceException` (fail closed, FR-033). Key prefix comes from `RedisKeyProperties`. Never logs the token. Makes T028 pass.
- [X] T035 [US2] Register the filter in `SecurityConfiguration.java` with `.addFilterAfter(revokedAccessTokenFilter, BearerTokenAuthenticationFilter.class)`, using the entry point from `ObjectProvider` with `BearerTokenAuthenticationEntryPoint` as fallback.
- [X] T036 [P] [US2] Create `RefreshRequest` and `LogoutRequest` in `identityaccess-presentation/src/main/java/<P>/presentation/authentication/` (`refreshToken` optional, max 512 characters; absent or blank is passed through so core treats it as an invalid credential), `toString()` omitting the value.
- [X] T037 [US2] Create `TokenController` in `identityaccess-presentation/src/main/java/<P>/presentation/authentication/TokenController.java`: `POST /api/v1/auth/refresh` (200 `TokenPairResponse`, `Cache-Control: no-store`) and `POST /api/v1/auth/logout` (always 204) per `contracts/http-api.md` sections 5-6. Makes T029 pass.
- [X] T038 [US2] End-to-end `SessionLifecycleE2eTest` in `identityaccess-start/src/test/java/<P>/start/e2e/SessionLifecycleE2eTest.java`: refresh gives a new pair; reusing the old refresh credential gives 401 and the session still renews with the new one; the session's expiry after refresh is later than before (SC-009); an expired/invalid `Authorization` header on `/auth/refresh` is ignored (FR-035); sign-in on two devices then logout of one makes its access token fail with 401 on the next request while the other device's access and refresh credentials work (SC-008); the revoked session cannot refresh (SC-007); after deleting the `identity-access:access-revoked:*` keys in Redis the session still cannot refresh (SC-010); with Redis stopped a protected request returns 503 `service_unavailable`.

**Checkpoint**: User Stories 1 and 2 both work.

---

## Phase 5: User Story 3 - See and Manage My Sessions (Priority: P2)

**Goal**: A signed-in person lists sessions, ends one with their password, or ends all others.

**Independent Test**: Quickstart section 7 and T042.

### Tests for User Story 3 (write first)

- [X] T039 [P] [US3] `SessionControllerTest` in `identityaccess-presentation/src/test/java/<P>/presentation/session/SessionControllerTest.java` (`@WebMvcTest` with a security test `jwt()` post-processor carrying `sub` and `sid`): list returns 200 array of objects with exactly `sessionId`, `deviceInfo`, `issuedAt`, `expiresAt`, `lastUsedAt`, `revokedAt` (no `userId`, no refresh credential) and uses `FindSessionsQuery.Command(userId from sub)`; revoke `Result(true)` gives 204, `Result(false)` gives 404 `session_not_found`, `AuthenticationFailedException` gives 403 `reauthentication_failed` (not 401); revoke-others passes the `sid` from the token as the current session, returns 200 `{"revokedCount":n}`, and maps `AuthenticationFailedException` to 403 `reauthentication_failed`; list with `AuthenticationFailedException` gives 403 `account_inaccessible`; no token gives 401 `unauthenticated`; a request body naming another user is ignored.

### Implementation for User Story 3

- [X] T040 [P] [US3] Create `SessionResponse` (`sessionId`, `deviceInfo`, `issuedAt`, `expiresAt`, `lastUsedAt` nullable, `revokedAt` nullable; no `userId`), `RevokeSessionRequest(currentPassword required, max 128)`, `RevokeOtherSessionsRequest(currentPassword required, max 128)`, `RevokedCountResponse(int revokedCount)` in `identityaccess-presentation/src/main/java/<P>/presentation/session/`; requests override `toString()`.
- [X] T041 [US3] Create `SessionController` in `identityaccess-presentation/src/main/java/<P>/presentation/session/SessionController.java` (`@RequestMapping("/api/v1/sessions")`): `GET` list, `POST /{sessionId}/revoke`, `POST /revoke-others` per `contracts/http-api.md` sections 7-9, taking the account and current session only from `AccessPrincipal.from(jwt)`. Translate `AuthenticationFailedException` from the revoke endpoints into the presentation exception that the handler maps to `reauthentication_failed` (add that exception class to `identityaccess-presentation/src/main/java/<P>/presentation/shared/error/ReauthenticationFailedException.java` and its handler mapping in `ApiExceptionHandler.java`); for the list endpoint map it to `account_inaccessible`. Makes T039 pass.
- [X] T042 [US3] End-to-end `SessionManagementE2eTest` in `identityaccess-start/src/test/java/<P>/start/e2e/SessionManagementE2eTest.java`: two sessions, list from one (only own sessions, newest first, no credentials), wrong password gives 403 and changes nothing, correct password revokes the other (its refresh and access credentials fail immediately), unknown or another account's session id gives 404 `session_not_found`, ending the current session is allowed, revoke-others spares the current session and repeated call returns `revokedCount` 0 and the ended sessions' access credentials fail immediately.

**Checkpoint**: Stories 1 to 3 work.

---

## Phase 6: User Story 4 - Manage My Own Account (Priority: P2)

**Goal**: Change the password and close the account over HTTP.

**Independent Test**: T046 and quickstart section 8.

### Tests for User Story 4 (write first)

- [X] T043 [P] [US4] `UserControllerTest` in `identityaccess-presentation/src/test/java/<P>/presentation/user/UserControllerTest.java`: `PUT /api/v1/users/@me/password` with `Result(true)` gives 204 and empty body, `Result(false)` gives 409 `password_unchanged`, `InvalidCredentialsException` gives 403 `reauthentication_failed`, `InvalidAccountStateTransitionException` gives 403 `account_inaccessible`, core `IllegalArgumentException` gives 400 `validation_failed` whose body contains neither password; `DELETE /api/v1/users/@me` gives 204 for both `Result(true)` and `Result(false)` and uses the id from `sub` only; unauthenticated gives 401.

### Implementation for User Story 4

- [X] T044 [P] [US4] Create `ChangePasswordRequest(currentPassword required, max 128; newPassword required, max 128)` in `identityaccess-presentation/src/main/java/<P>/presentation/user/ChangePasswordRequest.java`, `toString()` omitting values.
- [X] T045 [US4] Replace the stub `identityaccess-presentation/src/main/java/<P>/presentation/user/UserController.java` with `UserController` (`@RequestMapping("/api/v1/users")`): `PUT /@me/password` and `DELETE /@me` per `contracts/http-api.md` sections 10-11, identity from `AccessPrincipal.from(jwt)`. The old `DELETE /api/v1/users` path is removed. Makes T043 pass.
- [X] T046 [US4] End-to-end `AccountManagementE2eTest` in `identityaccess-start/src/test/java/<P>/start/e2e/AccountManagementE2eTest.java`: change password with the correct current password (204), the old access credential then fails with 401 (all sessions revoked), the new password signs in and the old one does not; wrong current password gives 403 `reauthentication_failed`; invalid new password gives 400 with a field error and no password in the body; close account (204), further sign-in gives 401, the account's access credential fails with 401, and closing again with a fresh credential is not possible (401).

**Checkpoint**: Stories 1 to 4 work.

---

## Phase 7: User Story 5 - Consistent Errors and Safe Logging (Priority: P1)

**Goal**: Prove every failure uses one shape, no internals leak, and no secret reaches logs or responses.

**Independent Test**: T049 and quickstart section 9.

- [X] T047 [P] [US5] `ApiExceptionHandlerTest` and `ErrorShapeContractTest` in `identityaccess-presentation/src/test/java/<P>/presentation/shared/error/`: drive each code in `contracts/error-model.md` through a test controller or the real controllers and assert status, `code`, fixed `message`, `timestamp` present, `errors` only on `validation_failed` and without rejected values; unexpected exception gives `internal_error` with a generic message and no class name or stack trace; the entry point and access-denied handler produce the same shape; 401 has `WWW-Authenticate: Bearer`, 503 has `Retry-After: 5`.
- [X] T048 [P] [US5] `SecretHandlingTest` in `identityaccess-presentation/src/test/java/<P>/presentation/structure/SecretHandlingTest.java`: reflectively find every request record in `..presentation..` and assert `toString()` of an instance populated with a sentinel contains none of the sentinel values; ArchUnit rule that no presentation class calls a logger with a request-record argument.
- [X] T049 [US5] End-to-end `SecretLeakageE2eTest` in `identityaccess-start/src/test/java/<P>/start/e2e/SecretLeakageE2eTest.java` using `OutputCaptureExtension`: run registration, verification, sign-in, refresh, logout, session management, password change, and every failure path with sentinel passwords, token, and refresh credentials; assert none appears in captured log output, and none appears in any response body except the refresh credential in the sign-in and refresh responses that issue it (SC-005). Also assert the application sets no request/response body logging and no access log that records the query string of `/api/v1/auth/verification/confirm`; fix configuration in `identityaccess-start/src/main/resources/application.yml` if it does.

**Checkpoint**: All five stories done and verified.

---

## Phase 8: Polish and Cross-Cutting Concerns

- [X] T050 [P] OpenAPI: create `OpenApiConfiguration` in `identityaccess-presentation/src/main/java/<P>/presentation/shared/openapi/OpenApiConfiguration.java` (title, version, bearer security scheme) and annotate all five controllers and request/response/`ApiError` records with `@Tag`, `@Operation`, `@ApiResponse`, and `@Schema` so every status and body in `contracts/http-api.md` appears (FR-028). Add `OpenApiE2eTest` in `identityaccess-start/src/test/java/<P>/start/e2e/OpenApiE2eTest.java`: `/v3/api-docs` is reachable without a credential, lists exactly the 11 operations in `contracts/http-api.md`, and `/swagger-ui.html` responds. This also proves springdoc 3.1.1 works with Spring Boot 4.0.8; if it does not, report and pick the matching springdoc release.
- [X] T051 [P] `AllowListE2eTest` in `identityaccess-start/src/test/java/<P>/start/e2e/AllowListE2eTest.java`: the public endpoints configured in `application.yml` plus the documentation paths equal the six public operations in `contracts/http-api.md`, and every other operation returns 401 without a credential (FR-003, SC-003: also missing, malformed, expired, tampered, and foreign-key tokens each give 401 in the standard shape).
- [X] T052 [P] Update earlier specs to match the new contracts: `specs/002-authentication-sessions/contracts/commands.md` ("Renew access" now rotates and returns both credentials, session expiry renews with no cap), `specs/002-authentication-sessions/contracts/ports.md` (`rotate` replaces `touchIfUsable`, `AccessTokenIssuer` takes the session id, revoke obligations), and `specs/003-infrastructure-adapters/contracts/adapter-map.md` plus `specs/003-infrastructure-adapters/contracts/configuration.md` (`sid` claim, `access-revoked` key, filter, bearer-token resolver, entry-point beans). Documentation only.
- [X] T053 Delete what the new packages replaced: confirm `identityaccess-presentation/src/main/java/<P>/presentation/user/` contains only the new `UserController.java` and `ChangePasswordRequest.java` (stubs `AuthController.java`, `RegisterRequest.java`, `ApiExceptionHandler.java` gone). Do NOT delete `identityaccess-presentation/src/main/java/<P>/presentation/user/.local/jwt-*.pem`: they are untracked development files; ask the user whether to remove them and report that nothing references them (`grep -rn "\.local" identityaccess-*/src` and `application.yml`).
- [X] T054 Run `./mvnw spotless:apply` then `./mvnw verify` from `services/identity-access-next/`; all modules must pass including the ArchUnit, Testcontainers, and end-to-end suites.
- [X] T055 Walk `specs/004-presentation-api/quickstart.md` sections 1-9 against the running jar and tick off each expected result; record any deviation in `specs/004-presentation-api/quickstart.md` as a correction.

---

## Dependencies and Execution Order

### Phase dependencies

- **Phase 1 Setup**: T001 first; T002 and T003 in parallel after it.
- **Phase 2 Foundational**: blocks all stories. Order: T004, T005, T006 sequential (same module, overlapping files); T007 after T006; T008 after T007. T009, T010, T012, T013, T015, T016 are independent of the core moves and can run in parallel with them (T010 needs only the Spring dependency from T002). T011 needs T004, T005, T009. T014 needs T013 and T012.
- **User stories** start after Phase 2 completes. Their code is independent of each other; their end-to-end tests call earlier stories' endpoints.
- **Polish**: after all stories.

### User story dependencies

- **US1**: needs Phase 2 only.
- **US2**: needs Phase 2; its end-to-end test (T038) needs US1's endpoints to create accounts, so run T025 first or reuse the harness helpers.
- **US3**: needs Phase 2; its end-to-end test needs US1 and US2 (revocation behavior).
- **US4**: needs Phase 2; its end-to-end test needs US1 and the revocation filter from US2 for the "old access credential fails" assertion.
- **US5**: T047 and T048 need only Phase 2; T049 needs the endpoints of all other stories.

### Within a story

Tests first and failing, then records/DTOs, then controllers or adapters, then the end-to-end test. In US2 the order is core (T026, T030), then Redis (T027, T031, T032, T033), then security (T028, T034, T035), then presentation (T029, T036, T037), then T038.

## Parallel Opportunities

- Phase 1: T002 and T003.
- Phase 2: T007, T009, T010, T012, T013, T015, T016 together once T006 is done (T007) or immediately (the rest).
- US1: T017, T018, T019 together; T020 and T021 together.
- US2: T026, T027, T028, T029 together (four different test files in three modules); T036 alongside T031 to T035.
- US3: T039 and T040 together. US4: T043 and T044 together.
- Different stories after Phase 2: US1, US3, and US4 code can be developed in parallel by different people; US2 touches the shared session repository and security files and should not overlap with another change to them.
- Polish: T050, T051, T052 together.

### Example: User Story 2 parallel start

```text
Task: T026 RefreshAccessTokenServiceTest + fakes      (identityaccess-core)
Task: T027 RedisAuthenticationSessionRepositoryTest   (identityaccess-infrastructure)
Task: T028 RevokedAccessTokenFilterTest               (identityaccess-infrastructure)
Task: T029 TokenControllerTest                        (identityaccess-presentation)
```

## Implementation Strategy

### MVP first (User Story 1)

1. Phase 1 and Phase 2 (includes the `sid` claim so every issued token is already compatible with revocation).
2. Phase 3 (US1). Stop and validate with T025 and quickstart section 4. This gives register, verify, and sign in.
3. Do not ship US1 alone beyond a development environment: without US2 there is no renewal or sign-out.

### Incremental delivery

1. Add US2 (renewal and immediate sign-out), which makes the sign-in usable for real clients. Validate with T038.
2. Add US3, then US4, each validated by its end-to-end test.
3. US5 verification closes the loop; Polish finishes documentation and the full build.

### Notes

- Constraints from `data-model.md` (field limits, optional refresh fields) are quoted in the tasks that create the records; do not loosen them.
- Do not add logging of request data anywhere; the only allowed log fields are the error code, exception class name, and correlation id.
- Every task that edits a Lua script must keep the file's header comment describing KEYS and ARGV in step with the code.
- Commit after each task or logical group; do not combine the core exception moves (T004, T005) with behavior changes in one commit.
