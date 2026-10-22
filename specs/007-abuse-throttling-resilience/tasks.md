---

description: "Task list for abuse throttling and outage resilience"
---

# Tasks: Abuse Throttling and Outage Resilience

**Input**: Design documents from `specs/007-abuse-throttling-resilience/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ (`http-surface.md`, `core-and-infra-changes.md`, `configuration.md`), quickstart.md

**Tests**: Included. The spec's success criteria (SC-001 to SC-009) are all behavioral, the plan lists the test classes, and SC-009 requires the existing suite to pass unchanged.

**Organization**: One new limiter port, its Redis adapter, the breaker wiring, and the new error codes are needed by every story, so they are built first in the Foundational phase. Each user story then adds its own rule on top. Stories US1 and US2 are both P1 but independent; US3 reuses files edited by US2 and US4 reuses files edited by US3 (called out per task).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1 to US6)
- Include exact file paths in descriptions

## Path Conventions

Repository root: `/home/minewaku/projects/java/2026/Chatter-identity-access-next`. Build commands run from `services/identity-access-next` with the per-service `./mvnw`, never a bare `mvn`. Git paths are relative to the repository root. Do not modify sibling services (`api-gateway`, `identity-access`, and the others).

Path shorthands used below (all relative to the repository root):

- `SVC` = `services/identity-access-next`
- `CORE_MAIN` = `SVC/identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess`
- `CORE_TEST` = `SVC/identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess`
- `INFRA_MAIN` = `SVC/identityaccess-infrastructure/src/main/java/com/minewaku/chatter/identityaccess/infrastructure`
- `INFRA_RES` = `SVC/identityaccess-infrastructure/src/main/resources`
- `INFRA_TEST` = `SVC/identityaccess-infrastructure/src/test/java/com/minewaku/chatter/identityaccess/infrastructure`
- `PRES_MAIN` = `SVC/identityaccess-presentation/src/main/java/com/minewaku/chatter/identityaccess/presentation`
- `PRES_TEST` = `SVC/identityaccess-presentation/src/test/java/com/minewaku/chatter/identityaccess/presentation`
- `START_RES` = `SVC/identityaccess-start/src/main/resources`
- `START_TEST` = `SVC/identityaccess-start/src/test/java/com/minewaku/chatter/identityaccess/start`

Two repository rules every task must respect: core imports no infrastructure, Redis, mail, or resilience type (`StructureRules` rule 1), and no log statement may mention `password`, `hash`, `token`, `credential` or similar words (`LoggingRuleTest`). No second `Duration` bean may be registered in the infrastructure module.

---

## Phase 1: Setup

**Purpose**: Establish that the starting point is green and add the one new dependency.

- [X] T001 From `SVC`, confirm Docker is up (`docker ps`) and run `./mvnw verify`; record that all modules pass before any edit. The Redis adapter tests and the e2e suite need the container stack.
- [X] T002 [P] In `SVC/pom.xml`, add the property `<resilience4j.version>2.4.0</resilience4j.version>` and a `dependencyManagement` entry for `io.github.resilience4j:resilience4j-circuitbreaker` using it. Do not add the Spring Boot starter, AOP, or any annotation module (research section 1).
- [X] T003 In `SVC/identityaccess-infrastructure/pom.xml`, declare `io.github.resilience4j:resilience4j-circuitbreaker` (version from T002). No other module may depend on it, and `identityaccess-core` must not.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The limiter port and adapter, the breaker wiring, the new exceptions and error codes, and base configuration. No user story can start until this phase is complete.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Core types

- [X] T004 [P] Create `CORE_MAIN/shared/throttle/AttemptPolicy.java`: `record AttemptPolicy(String name, int maxAttempts, Duration window)`. Constructor validation: `maxAttempts >= 1`, `window` positive, `name` nonblank; throw `IllegalArgumentException`.
- [X] T005 [P] Create `CORE_MAIN/shared/throttle/AttemptOutcome.java`: `record AttemptOutcome(boolean allowed, Duration retryAfter)`. `retryAfter` is the remaining window, meaningful when `allowed` is false, and never longer than the policy window.
- [X] T006 [P] Create `CORE_MAIN/shared/throttle/ThrottlePolicies.java`: `record ThrottlePolicies(AttemptPolicy signIn, AttemptPolicy reconfirmation, AttemptPolicy resendGap, AttemptPolicy resendHourly, AttemptPolicy emailChangeRequest)`. Core only declares the type; infrastructure provides the bean (T015).
- [X] T007 [P] Create `CORE_MAIN/shared/exception/TooManyAttemptsException.java`: `final class extends RuntimeException`, carries `Duration retryAfter()`, no message that could carry a subject or address.
- [X] T008 [P] Create `CORE_MAIN/shared/exception/DependencyUnavailableException.java`: same shape as T007, for a protective store that could not be used.
- [X] T009 [P] Create `CORE_MAIN/shared/exception/MailDeliveryException.java`: `class extends RuntimeException`, no payload, thrown by both mail notifier adapters (not final, so the adapters may add a cause).
- [X] T010 Create `CORE_MAIN/shared/port/AttemptLimiter.java` per `contracts/core-and-infra-changes.md`: `AttemptOutcome hit(AttemptPolicy policy, String subject)` and `void clear(AttemptPolicy policy, String subject)`. Javadoc must say it throws `DependencyUnavailableException` when it cannot check and never returns "allowed" because it could not check. Depends on T004, T005.
- [X] T011 [P] Create `CORE_TEST/shared/throttle/InMemoryAttemptLimiter.java`, a fake implementing `AttemptLimiter` with a settable `Clock`-free window (tests advance it explicitly), counting per `(policy.name(), subject)`, with the same observable rules as the Redis adapter: first hit allowed, over `maxAttempts` refused with positive `retryAfter` no longer than the window, `clear` resets, different subjects and policies independent, and a test switch that makes it throw `DependencyUnavailableException`.
- [X] T012 [P] Create `CORE_TEST/shared/throttle/InMemoryAttemptLimiterTest.java` pinning those rules, so the fake is trustworthy.

### Infrastructure: limiter and breakers

- [X] T013 [P] Create `INFRA_RES/redis/throttle-hit.lua` exactly as in `contracts/core-and-infra-changes.md`: `KEYS[1]` counter key, `ARGV[1]` window in milliseconds; `INCR`; `PEXPIRE` on the first hit; if `PTTL` is negative set `PEXPIRE` again; return `{ count, remainingMillis }`. The expiry is never extended by later hits.
- [X] T014 Create `INFRA_MAIN/adapter/outbound/throttle/ThrottleProperties.java`: `@ConfigurationProperties("identity-access.throttle")` record with the five policies and the defaults from `data-model.md`: `sign-in` 5 per `PT15M`, `reconfirmation` 5 per `PT15M`, `resend-gap` 1 per `PT1M`, `resend-hourly` 5 per `PT1H`, `email-change-request` 3 per `PT1H`. Startup validation (same style as `SessionProperties`): `max-attempts >= 1`, `window` positive, and every policy has a distinct name; the message names the property.
- [X] T015 Create `INFRA_MAIN/adapter/outbound/throttle/ThrottleConfiguration.java` exposing the `ThrottlePolicies` bean built from `ThrottleProperties` (use the policy names `sign-in`, `reconfirmation`, `resend-gap`, `resend-hourly`, `email-change-request`). Add no `Duration` bean. Depends on T006, T014.
- [X] T016 [P] Create `INFRA_MAIN/integration/resilience/ResilienceProperties.java`: `@ConfigurationProperties("identity-access.resilience")` record `(BreakerProperties mail, BreakerProperties sessionStore)` with `window-size`, `minimum-calls`, `failure-rate-percent`, `open-duration`, `trial-calls`. Defaults: mail 10, 5, 50, `PT30S`, 2; session-store 20, 10, 50, `PT10S`, 3. Validation: `window-size >= 1`; `minimum-calls` between 1 and `window-size`; `failure-rate-percent` between 1 and 100; `open-duration > 0`; `trial-calls >= 1`.
- [X] T017 Create `INFRA_MAIN/integration/resilience/ResilienceConfiguration.java`: a `CircuitBreakerRegistry` bean and two named `CircuitBreaker` beans, `mailBreaker` and `sessionStoreBreaker`, count-based sliding window built from `ResilienceProperties`. Log state transitions at `warn` with the breaker name and the two states only. Add no `Duration` bean. Depends on T003, T016.
- [X] T018 Create `INFRA_MAIN/adapter/outbound/throttle/RedisAttemptLimiter.java` (`@Repository`, package-private constructor taking `StringRedisTemplate`, `RedisKeyProperties`, and the `sessionStoreBreaker` by `@Qualifier`). Key: `keys.keyPrefix() + "throttle:" + policy.name() + ":" + TokenDigest.sha256Hex(subject)`. `hit` runs `throttle-hit.lua` (loaded with `RedisScripts.load("throttle-hit.lua", List.class)`) inside the breaker and returns `allowed = count <= policy.maxAttempts()` and `retryAfter` from the remaining milliseconds capped at the window. `clear` is `DEL` inside the breaker. `CallNotPermittedException` and `DataAccessException` become `DependencyUnavailableException` with `retryAfter` equal to the breaker's open duration. Never log the subject. Depends on T010, T013, T015, T017.
- [X] T019 In `INFRA_TEST/structure/StructureRules.java` add the `PORT_MAP` row `row("shared.port.AttemptLimiter", "RedisAttemptLimiter", "throttle")`. `PortAdapterMapTest` must then pass (exactly one adapter, in `adapter.outbound.throttle`).
- [X] T020 [P] Create `INFRA_TEST/adapter/outbound/throttle/ThrottlePropertiesTest.java`: defaults equal the values in T014, an invalid value (zero `max-attempts`, zero `window`, a duplicate name) fails startup with a message naming the property.
- [X] T021 [P] Create `INFRA_TEST/integration/resilience/ResiliencePropertiesTest.java`: defaults equal the values in T016, each invalid value in T016 fails startup with a message naming the property.
- [X] T022 Create `INFRA_TEST/adapter/outbound/throttle/RedisAttemptLimiterTest.java` on `RedisTestSupport`: first hit allowed; hit `max + 1` refused with positive `retryAfter` no longer than the window; a refused hit does not extend the expiry (check the remaining TTL does not grow); `clear` resets; different subjects and different policies are independent; the stored key contains no part of the raw subject; 50 concurrent hits against a limit of 10 allow exactly 10; a paused Redis gives `DependencyUnavailableException`; an open breaker gives it without touching the store. Depends on T018.

### Presentation: error codes

- [X] T023 [P] In `PRES_MAIN/shared/error/ErrorCode.java` add `TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Try again later.")` and `MAIL_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The email could not be sent right now. Try again later.")`. Messages are fixed and never echo input.
- [X] T024 In `PRES_MAIN/shared/error/ApiErrors.java` add `response(ErrorCode code, Duration retryAfter)` that sets `Retry-After` to whole seconds, rounded up, at least 1. Depends on T023.
- [X] T025 In `PRES_MAIN/shared/error/ApiExceptionHandler.java` add handlers: `TooManyAttemptsException` to `429 too_many_attempts` with `Retry-After`; `DependencyUnavailableException` to `503 service_unavailable` with `Retry-After`; `MailDeliveryException` to `503 mail_unavailable` with a fixed `Retry-After` of `30`. None of the three extends `IllegalArgumentException`, so the existing handlers do not catch them first. Depends on T007, T008, T009, T023, T024.
- [X] T026 In `PRES_TEST/shared/error/ApiExceptionHandlerTest.java` and `PRES_TEST/shared/error/ErrorProbeController.java` add cases for the three new exceptions: status, code, fixed message, `Retry-After` value (including rounding up and the minimum of 1), and no submitted value in the body. Depends on T025.

### Configuration

- [X] T027 In `START_RES/application.yml` add `identity-access.throttle.*` and `identity-access.resilience.*` with the defaults from T014 and T016 as written in `contracts/configuration.md`, and under `spring.data.redis` add `timeout: 1s` and `connect-timeout: 1s`.
- [X] T028 In `START_TEST/e2e/E2eSupport.java` add dynamic properties that set every throttle policy to `max-attempts` 1000 with a `PT1H` window (so the existing end-to-end tests are unaffected, FR-023) and change `spring.data.redis.timeout` from `2s` to `1s` (matching production). Add an autowired `CircuitBreakerRegistry` field and a protected `resetBreakers()` helper that calls `reset()` on every breaker in it, for later tests that open a breaker. Depends on T017, T027.

**Checkpoint**: From `SVC`, `./mvnw -pl identityaccess-core,identityaccess-infrastructure,identityaccess-presentation test` passes, and `./mvnw -pl identityaccess-start verify` still passes with no behavior change. The limiter exists but nothing calls it yet.

---

## Phase 3: User Story 1 - Repeated Failed Sign-Ins Are Throttled Per Account (Priority: P1) 🎯 MVP

**Goal**: After 5 attempts for one email within `PT15M`, further sign-in attempts for it are refused as `429 too_many_attempts` with a wait time, from any caller, even with the correct password. A success clears the count. Unknown emails behave identically.

**Independent Test**: Make 5 wrong sign-ins against one account from different callers, then a 6th with the correct password: it is `429` with `Retry-After`. The same for an email with no account. After the window, a correct sign-in works.

### Tests for User Story 1

- [X] T029 [P] [US1] In `CORE_TEST/authentication/internal/application/AuthenticationTestFakes.java` wire `InMemoryAttemptLimiter` and a `ThrottlePolicies` with `signIn` = 5 per `PT15M` into the way `AuthenticateService` is constructed in tests.
- [X] T030 [US1] In `CORE_TEST/authentication/internal/application/AuthenticateServiceTest.java` add: five wrong passwords for one email, then the sixth with the right password throws `TooManyAttemptsException` with positive `retryAfter`; the same limit applies across different device labels; an unknown email gets the same exception after five attempts; a success after fewer than five failures clears the count (five more failures are then allowed); a malformed email still throws `AuthenticationFailedException` and does not count; a limiter that throws `DependencyUnavailableException` makes sign-in throw it (fail closed). Depends on T029.

### Implementation for User Story 1

- [X] T031 [US1] In `CORE_MAIN/authentication/internal/application/command/AuthenticateService.java` inject `AttemptLimiter` and `ThrottlePolicies` (both `@NonNull`, constructor via `@RequiredArgsConstructor` as today). After the email is parsed and before the account lookup, call `hit(policies.signIn(), email.value())` and throw `TooManyAttemptsException(outcome.retryAfter())` if not allowed. After a successful match call `clear`. A malformed email takes the existing path (no hit). The hit must happen outside the `@Transactional` rollback concern: it is a Redis call, so it survives the rollback of a failed sign-in. Depends on T010, T030.
- [X] T032 [P] [US1] In `PRES_MAIN/authentication/web/AuthController.java` add `ErrorCode.TOO_MANY_ATTEMPTS` and `ErrorCode.SERVICE_UNAVAILABLE` to the `@DocumentedErrors` of `authenticate`.
- [X] T033 [US1] In `PRES_TEST/authentication/web/AuthControllerTest.java` add a case where the mocked use case throws `TooManyAttemptsException`: status `429`, code `too_many_attempts`, `Retry-After` present, body has no email or password. Depends on T025, T032.
- [X] T034 [US1] Create `START_TEST/e2e/ThrottlingE2eTest.java` extending `E2eSupport`, with `@TestPropertySource` overriding `identity-access.throttle.sign-in` to `max-attempts` 3 and `window` `PT3S` (its own context; the other policies stay at 1000). Add the sign-in scenario: three wrong sign-ins give `401`, the fourth (with the correct password) gives `429` with `Retry-After`; an unknown email gives the same `429` body shape; after the window a correct sign-in gives `200`; a success clears the count. Depends on T028, T031.

**Checkpoint**: User Story 1 works on its own. `./mvnw -pl identityaccess-core,identityaccess-presentation test` and the new e2e test pass.

---

## Phase 4: User Story 2 - Password Confirmation Cannot Be Brute-Forced With A Stolen Credential (Priority: P1)

**Goal**: One count per signed-in person across change password, close account, email-change request, and the two session commands that confirm a password: 5 wrong confirmations within `PT15M` block all of them with `429`; a wrong or blocked confirmation never ends the session; a correct one clears the count.

**Independent Test**: Sign in, send 5 wrong passwords to any of those actions, then the correct one to a different one of them: `429`, session still valid. After the window the correct password works.

### Tests for User Story 2

- [X] T035 [P] [US2] Create `CORE_TEST/user/internal/application/ReauthenticationGuardTest.java` with `InMemoryAttemptLimiter`: `confirm` runs the action when allowed; an action throwing `InvalidCredentialsException` or `AuthenticationFailedException` is rethrown and the count stays; an action that returns normally clears the count; an action throwing any other exception (for example a conflict) clears the count and rethrows; once five wrong attempts are recorded the action is not run and `TooManyAttemptsException` is thrown; different users are independent; a limiter that throws `DependencyUnavailableException` stops the action (fail closed).
- [X] T036 [P] [US2] In `CORE_TEST/user/internal/application/UserAccountTestFakes.java` wire `InMemoryAttemptLimiter` and a `ThrottlePolicies` with `reconfirmation` = 5 per `PT15M` into the service constructors used by `UserAccountApplicationServiceTest` and `EmailChangeApplicationServiceTest`.
- [X] T037 [US2] In `CORE_TEST/authentication/internal/application/AuthenticationTestFakes.java` add the same wiring for `LogoutSessionService` and `LogoutOtherSessionsService` (edits the same file as T029, so do T029 first).

### Implementation for User Story 2

- [X] T038 [US2] Create `CORE_MAIN/user/internal/application/ReauthenticationGuard.java` (`@Component`) per `contracts/core-and-infra-changes.md`: constructor `(AttemptLimiter limiter, ThrottlePolicies policies)`; `public <R> R confirm(long userId, Supplier<R> action)`: `hit(policies.reconfirmation(), Long.toString(userId))`, throw `TooManyAttemptsException(retryAfter)` if not allowed without running the action, run the action, on `InvalidCredentialsException` or `AuthenticationFailedException` rethrow and keep the count, otherwise `clear` and pass the outcome or the other exception through. Depends on T010, T035.
- [X] T039 [US2] In `CORE_MAIN/authentication/internal/application/AccountVerifier.java` take a `ReauthenticationGuard` in the constructor and run the existing body of `verify` through `guard.confirm(userId, ...)`, so a wrong password or inaccessible account (`AuthenticationFailedException`) counts. Depends on T038.
- [X] T040 [US2] In `CORE_MAIN/authentication/internal/application/command/LogoutSessionService.java` inject `ReauthenticationGuard` and pass it to `new AccountVerifier(...)`. Depends on T039.
- [X] T041 [US2] In `CORE_MAIN/authentication/internal/application/command/LogoutOtherSessionsService.java` inject `ReauthenticationGuard` and pass it to `new AccountVerifier(...)`. Depends on T039.
- [X] T042 [US2] In `CORE_MAIN/user/internal/application/command/ChangePasswordService.java` inject `ReauthenticationGuard` and run the body of `handle` through `confirm(command.userId(), ...)`. Keep `@Transactional`. Depends on T038.
- [X] T043 [US2] In `CORE_MAIN/user/internal/application/command/SoftDeleteUserAccountService.java` inject `ReauthenticationGuard` and run the body of `handle` through `confirm(command.userId(), ...)`. Keep `@Transactional`. Depends on T038.
- [X] T044 [US2] In `CORE_MAIN/user/internal/application/command/RequestEmailChangeService.java` inject `ReauthenticationGuard` and run the whole body of `handle` through `confirm(command.userId(), ...)`. Depends on T038.
- [X] T045 [US2] Update `CORE_TEST/user/internal/application/UserAccountApplicationServiceTest.java`, `CORE_TEST/user/internal/application/EmailChangeApplicationServiceTest.java`, `CORE_TEST/authentication/internal/application/LogoutSessionServiceTest.java`, and `CORE_TEST/authentication/internal/application/LogoutOtherSessionsServiceTest.java` for the new constructors, and add per action: five wrong passwords then the correct one gives `TooManyAttemptsException`; the count is shared across the different actions (wrong passwords on change password block close account); a correct password clears it; the account and sessions are untouched when blocked. Depends on T036, T037, T040 to T044.
- [X] T046 [P] [US2] In `PRES_MAIN/user/web/UserController.java` add `ErrorCode.TOO_MANY_ATTEMPTS` to the `@DocumentedErrors` of `changePassword`, `requestEmailChange` and `close`; in `PRES_MAIN/session/web/SessionController.java` add `TOO_MANY_ATTEMPTS` to `revoke` and `revokeOthers` and `SERVICE_UNAVAILABLE` where missing. `SessionController` catches only `AuthenticationFailedException`, so the new exceptions pass through; leave the catch as is.
- [X] T047 [US2] In `PRES_TEST/user/web/UserControllerTest.java` and `PRES_TEST/session/web/SessionControllerTest.java` add a case per affected endpoint where the mocked use case throws `TooManyAttemptsException`: `429`, code `too_many_attempts`, `Retry-After`, session-independent (no `401`). Depends on T025, T046.
- [X] T048 [US2] In `START_TEST/e2e/ThrottlingE2eTest.java` add `reconfirmation` to the `@TestPropertySource` with `max-attempts` 3 and window `PT3S`, and the confirmation scenario: three wrong passwords across change password and close account give `403 reauthentication_failed`; the next confirmation (also on a third action, `revoke-others`) gives `429` with `Retry-After`; the access credential still works afterwards (`GET /sessions` is `200`); after the window the correct password succeeds. Depends on T034 (same file), T042 to T044, T040, T041.

**Checkpoint**: User Stories 1 and 2 both work independently.

---

## Phase 5: User Story 3 - Verification And Email-Change Mail Cannot Flood An Inbox Or Exhaust Mail Quota (Priority: P2)

**Goal**: Resend is limited per address (1 per `PT1M`, then 5 per `PT1H`), email-change requests per person (3 per `PT1H`). A refusal is `429 too_many_attempts` and sends no mail; a refusal never reveals whether an address has an account.

**Independent Test**: Request a resend twice in a row: the second is `429` and no extra mail is sent. Repeat past the hourly cap: `429`. Do the same for a non-existent address: identical responses. Make 4 email-change requests in an hour: the fourth is `429` and nothing is mailed.

### Tests for User Story 3

- [X] T049 [P] [US3] In `CORE_TEST/emailverification/internal/application/EmailVerificationTestFakes.java` wire `InMemoryAttemptLimiter` and a `ThrottlePolicies` with `resendGap` = 1 per `PT1M` and `resendHourly` = 5 per `PT1H`.
- [X] T050 [US3] In `CORE_TEST/emailverification/internal/application/RequestEmailVerificationServiceTest.java` add: a second request inside the gap throws `TooManyAttemptsException` and sends nothing; after the gap the next is accepted; the sixth request in the hour is refused; a request refused by the gap does not consume the hourly count; an address with no account is throttled exactly like a real one (same exception, same `retryAfter` rules); a limiter that throws `DependencyUnavailableException` stops the request. Depends on T049.
- [X] T051 [P] [US3] In `CORE_TEST/user/internal/application/EmailChangeApplicationServiceTest.java` add: the fourth email-change request within the hour throws `TooManyAttemptsException` and issues and mails nothing; a wrong password, an unchanged address, or an address already taken does not consume the `emailChangeRequest` count. Depends on T036 (same wiring), T045.

### Implementation for User Story 3

- [X] T052 [US3] In `CORE_MAIN/emailverification/internal/application/RequestEmailVerificationService.java` inject `AttemptLimiter` and `ThrottlePolicies`. After `new Email(command.email())`: `hit(policies.resendGap(), email.value())` and refuse with `TooManyAttemptsException` if not allowed; only then `hit(policies.resendHourly(), email.value())` and refuse likewise; then the existing lookup and issue. Apply to every well-formed address whether or not it has an account. Depends on T010, T050.
- [X] T053 [US3] In `CORE_MAIN/user/internal/application/command/RequestEmailChangeService.java` inject `AttemptLimiter` and `ThrottlePolicies` and, after the password is accepted and the new address is confirmed free and just before `issuer.issue(...)`, call `hit(policies.emailChangeRequest(), Long.toString(account.id().value()))`, throwing `TooManyAttemptsException` if not allowed. Depends on T044 (same file), T051.
- [X] T054 [P] [US3] In `PRES_MAIN/verification/web/VerificationController.java` add `ErrorCode.TOO_MANY_ATTEMPTS` to the `@DocumentedErrors` of `resend` and change the `@ApiResponse` description of `202` so it no longer says it is "always" accepted (it is accepted unless throttled).
- [X] T055 [US3] In `PRES_TEST/verification/web/VerificationControllerTest.java` add a case where the mocked use case throws `TooManyAttemptsException`: `429` with `Retry-After`. Depends on T025, T054.
- [X] T056 [US3] In `START_TEST/e2e/ThrottlingE2eTest.java` add `resend-gap` (1 per `PT3S`), `resend-hourly` (3 per `PT1H`) and `email-change-request` (2 per `PT1H`) to the `@TestPropertySource` and the scenario: two resends in a row give `202` then `429` with no extra mail (`mail.countFor` unchanged); resend to an unknown address gives the same `429` as for a pending one; the hourly cap gives `429` after the gap passes; two email-change requests succeed and the third gives `429` with no mail to the new address. Depends on T048 (same file), T052, T053.

**Checkpoint**: User Stories 1, 2 and 3 work independently.

---

## Phase 6: User Story 4 - A Failing Mail Provider Does Not Break Sign-Up And Does Not Pile Up Waiting Requests (Priority: P2)

**Goal**: Mail goes through one dispatcher with a circuit breaker outside the existing retry. Registration keeps the pending account and answers `201` with `verificationMailSent: false`; resend stays a blind `202`; an email-change request answers `503 mail_unavailable` with the email unchanged; sending resumes automatically after the hold-off.

**Independent Test**: With the sender failing, register: `201`, `verificationMailSent: false`, account pending, answered within 2 seconds once the breaker is open. Email change: `503 mail_unavailable`. After the sender recovers and the open duration passes, resend delivers mail with no restart.

### Tests for User Story 4

- [X] T057 [P] [US4] In `START_TEST/e2e/RecordingMailSender.java` add a switch `failSending(boolean)`; when on, `doSend` throws `org.springframework.mail.MailSendException` instead of recording.
- [X] T058 [P] [US4] Create `INFRA_TEST/adapter/outbound/verification/mail/MailDispatcherTest.java`: a send that succeeds calls the sender once; a send that fails is retried 3 times (200 ms, multiplier 2) and counts as one breaker failure, not four; after the configured failures the breaker opens and `send` throws `MailDeliveryException` without calling the sender; after the open duration a good send closes it; a message that fails to send never has its body, recipient, token or link in the exception message or in captured log output. Verify whether `RetryTemplate.invoke(Runnable)` rethrows the original exception or a wrapper (research section 7) and assert the dispatcher always surfaces `MailDeliveryException`.
- [X] T059 [P] [US4] In `CORE_TEST/user/internal/application/UserAccountApplicationServiceTest.java` add: when the verification notifier fake throws `MailDeliveryException`, `RegisterUserService` still saves the account (status pending) and returns `Registered(userId, false)`; on success it returns `Registered(userId, true)`. Depends on T045 (same file).
- [X] T060 [P] [US4] In `CORE_TEST/emailverification/internal/application/RequestEmailVerificationServiceTest.java` add: when the notifier throws `MailDeliveryException` the service still returns `Accepted`. Depends on T050 (same file).
- [X] T061 [P] [US4] In `CORE_TEST/user/internal/application/EmailChangeApplicationServiceTest.java` add: when the email-change notifier throws `MailDeliveryException` it propagates, the account's email is unchanged, and no confirmation can succeed with a token the person never received. Depends on T051 (same file).

### Implementation for User Story 4

- [X] T062 [US4] Create `INFRA_MAIN/adapter/outbound/verification/mail/MailDispatcher.java` (package-private `@Component`, constructor `(JavaMailSender sender, @Qualifier("mailBreaker") CircuitBreaker breaker)`): builds a Spring `RetryTemplate` (`RetryPolicy.builder().maxRetries(3).delay(Duration.ofMillis(200)).multiplier(2).includes(MailException.class).build()`) and exposes `void send(MimeMessage message)` = `breaker.executeRunnable(() -> retry.invoke(() -> sender.send(message)))`. `CallNotPermittedException` and any exception after the retries become `MailDeliveryException` with the cause attached and a fixed message that carries no address, token, or link. Depends on T003, T009, T017, T058.
- [X] T063 [US4] In `INFRA_MAIN/adapter/outbound/verification/mail/MailVerificationNotifier.java` remove `@Retryable`, inject `MailDispatcher`, keep building the message, call `dispatcher.send(message)`, and turn a `MessagingException` from building the message into `MailDeliveryException`. Depends on T062.
- [X] T064 [US4] In `INFRA_MAIN/adapter/outbound/verification/mail/MailEmailChangeNotifier.java` make the same change as T063. Depends on T062.
- [X] T065 [US4] Delete `INFRA_MAIN/integration/mail/MailConfiguration.java` (nothing left needs `@EnableResilientMethods`) and delete `INFRA_MAIN/adapter/outbound/verification/mail/VerificationMailException.java` if no reference remains after T063 and T064 (search first with `grep -rn VerificationMailException SVC`); update `INFRA_TEST/adapter/outbound/verification/mail/MailVerificationNotifierTest.java` and `INFRA_TEST/adapter/outbound/verification/mail/MailEmailChangeNotifierTest.java` to the new wiring (they currently `@Import(MailConfiguration.class)` and assert the retry count through the proxy; move the retry assertions to `MailDispatcherTest` and keep only message content, recipient, subject, and "no secrets in captured output" checks, plus that a failing sender yields `MailDeliveryException`). Depends on T063, T064.
- [X] T066 [US4] In `CORE_MAIN/user/api/command/RegisterUserUseCase.java` change `Registered` to `record Registered(long userId, boolean verificationMailSent) implements Result {}`; fix every `new Registered(...)` in `SVC` (search with `grep -rn "Registered(" SVC --include=*.java`), including tests.
- [X] T067 [US4] In `CORE_MAIN/user/internal/application/command/RegisterUserService.java` wrap `verificationIssuance.issue(...)` in a try/catch for `MailDeliveryException` only; return `new Registered(account.id().value(), false)` on failure and `new Registered(account.id().value(), true)` otherwise, so the `@Transactional` boundary commits the pending account. Depends on T009, T059, T066.
- [X] T068 [US4] In `CORE_MAIN/emailverification/internal/application/RequestEmailVerificationService.java` catch `MailDeliveryException` around the issuance and return `Accepted` anyway (a `503` only for pending accounts would reveal which addresses are pending; research section 8). Depends on T052 (same file), T060.
- [X] T069 [P] [US4] In `PRES_MAIN/authentication/dto/RegisterResponse.java` change to `record RegisterResponse(long userId, boolean verificationMailSent)`; in `PRES_MAIN/authentication/web/AuthController.java` pass `registered.verificationMailSent()` in `register`, and document the new field in the `@Content` schema of the `201` response. Depends on T066. (`AuthController.java` was also edited in T032; do T032 first.)
- [X] T070 [P] [US4] In `PRES_MAIN/user/web/UserController.java` add `ErrorCode.MAIL_UNAVAILABLE` to the `@DocumentedErrors` of `requestEmailChange`. Depends on T046 (same file).
- [X] T071 [US4] In `PRES_TEST/authentication/web/AuthControllerTest.java` assert the register response body carries `verificationMailSent` both when `true` and when `false`; in `PRES_TEST/user/web/UserControllerTest.java` add a case where the use case throws `MailDeliveryException`: `503`, code `mail_unavailable`, `Retry-After`. Depends on T033, T047 (same files), T069, T070.
- [X] T072 [US4] Create `START_TEST/e2e/MailOutageE2eTest.java` extending `E2eSupport`, with an `@AfterEach` that calls `mail.failSending(false)` and `resetBreakers()` (T028) so later tests are unaffected. Scenarios: with the sender failing, a registration gives `201` with `verificationMailSent: false` and the account is pending (a later verified flow works after recovery); after enough failed sends to open the breaker (use `@TestPropertySource` to set `identity-access.resilience.mail` `window-size` 3, `minimum-calls` 3, `open-duration` `PT3S`), a registration answers within 2 seconds; resend still gives `202`; email change gives `503 mail_unavailable` and the account's email is unchanged; after the sender recovers and the open duration passes, a resend delivers mail without a restart. Depends on T057, T067, T068, T063, T064.

**Checkpoint**: User Stories 1 to 4 work independently.

---

## Phase 7: User Story 5 - A Slow Or Down Session Store Fails Fast And Safely (Priority: P3)

**Goal**: The revocation lookup has a bounded wait (1 s Redis timeout from T027) and sits inside the `sessionStore` breaker: after repeated failures requests are refused as `503` immediately for the open duration, never allowed through, and normal checking resumes automatically.

**Independent Test**: Pause Redis and send signed-in requests: every one is `503` within 2 seconds and none succeed; after unpausing and the open duration, requests succeed with no restart.

### Tests for User Story 5

- [X] T073 [P] [US5] In `INFRA_TEST/security/RevokedAccessTokenFilterTest.java` add: with a breaker that is open (or opens after the configured failures) a request is rejected through the entry point with `AuthenticationServiceException` (mapped to `503`) without a store call; a revoked session still gets `401`; a healthy store passes the request; a revoked hit is a successful call and does not count against the breaker. Existing cases that build the filter without a breaker keep passing.

### Implementation for User Story 5

- [X] T074 [US5] In `INFRA_MAIN/security/RevokedAccessTokenFilter.java` add an optional `CircuitBreaker` constructor parameter (keep the existing constructor delegating with `null`); when present, run `redis.hasKey(...)` through `breaker.executeSupplier(...)`. Leave the existing `catch (RuntimeException e)` untouched: `CallNotPermittedException` is a `RuntimeException`, so it already becomes `AuthenticationServiceException` and `503`. Depends on T017, T073.
- [X] T075 [US5] In `INFRA_MAIN/security/SecurityConfiguration.java` add `@Qualifier("sessionStoreBreaker") ObjectProvider<CircuitBreaker>` to `securityFilterChain` and pass `breaker.getIfAvailable()` into the filter. Two `CircuitBreaker` beans exist, so the qualifier is required. Chain tests that do not define the bean keep working. Depends on T074.
- [X] T076 [US5] In `START_TEST/e2e/RevocationStoreFailureE2eTest.java` extend the existing test: assert the paused-store request completes within 2 seconds and has `503` and `Retry-After`; add a second scenario that sets `identity-access.resilience.session-store` to `window-size` 3, `minimum-calls` 3, `open-duration` `PT3S` through `@TestPropertySource`, sends enough requests while paused to open the breaker, asserts the next request is `503` in well under the Redis timeout, then unpauses, waits past the open duration, and asserts a signed-in request succeeds; call `resetBreakers()` in `@AfterEach`. Depends on T028, T075.
- [X] T077 [US5] In `START_TEST/e2e/ThrottlingE2eTest.java` add the fail-closed case (FR-014): with Redis paused, a sign-in answers `503 service_unavailable` and never succeeds or behaves as "unlimited"; unpause in a `finally` and call `resetBreakers()`. Depends on T056 (same file), T031.

**Checkpoint**: User Stories 1 to 5 work independently.

---

## Phase 8: User Story 6 - Operators Can See Dependency Health (Priority: P3)

**Goal**: The existing health endpoint shows `mailDelivery` and `sessionStore` components, `UP` when the breaker is closed and `DEGRADED` otherwise, with no details.

**Independent Test**: Open each breaker and read `/actuator/health`: the component shows `DEGRADED` and the overall response stays HTTP 200; close it and it shows `UP` again.

### Tests for User Story 6

- [X] T078 [P] [US6] Create `INFRA_TEST/integration/resilience/DependencyHealthIndicatorsTest.java`: closed breaker gives `Status.UP`; open and half-open give `Status("DEGRADED")`; the result has no details (no credentials, recipients, or addresses); the two components are named `mailDelivery` and `sessionStore`.

### Implementation for User Story 6

- [X] T079 [US6] Create `INFRA_MAIN/integration/resilience/DependencyHealthIndicators.java` with two `HealthIndicator` beans (`mailDelivery` over `mailBreaker`, `sessionStore` over `sessionStoreBreaker`) reporting `Health.up()` when `CLOSED` and `Health.status("DEGRADED").build()` otherwise. Depends on T017, T078.
- [X] T080 [US6] In `START_RES/application.yml` under `management` add `endpoint.health.show-components: always` and `endpoint.health.status.order: DOWN,OUT_OF_SERVICE,DEGRADED,UP,UNKNOWN` and `endpoint.health.status.http-mapping.DEGRADED: 200`; keep `management.health.mail.enabled: false` and `management.endpoints.web.exposure.include: health` unchanged. Depends on T027 (same file).
- [X] T081 [US6] In `START_TEST/e2e/StartSmokeE2eTest.java` add an assertion that `GET /actuator/health` (check how `ApiClient` builds URLs; the health path is not under `/api/v1`) is `200` and lists `mailDelivery` and `sessionStore` as `UP`, with no `details` field on either. Depends on T079, T080.

**Checkpoint**: All six user stories work independently.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Documentation of changed contracts, regression net, and format.

- [X] T082 [P] Add a one-line note "amended by 007: resend can answer `429 too_many_attempts`" to the resend entry in `specs/004-presentation-api/contracts/http-api.md`, where it says resend is always accepted.
- [X] T083 [P] In `START_TEST/e2e/OpenApiE2eTest.java` assert that the new codes `too_many_attempts` and `mail_unavailable` appear on the endpoints in `contracts/http-surface.md` (authenticate, resend, change password, close, email change, session revoke, revoke-others) and that the register `201` schema has `verificationMailSent`. Depends on T032, T046, T054, T069, T070.
- [X] T084 Run `./mvnw spotless:apply` from `SVC`, then `./mvnw spotless:check`; review that only files from this task list changed (`git status --short -- services specs`).
- [X] T085 Run `./mvnw verify` from `SVC` and confirm every module is green, including `PortAdapterMapTest`, `ArchitectureTest`, `LoggingRuleTest`, and the unchanged existing e2e tests (SC-009). If an existing test fails because it trips a limit or a breaker, fix the test configuration, not the production default.
- [X] T086 Run the manual check in `quickstart.md` section 4 against a locally running service (five wrong sign-ins then `429`; health components; pause and unpause Redis) and record the outcome in the pull request description.
- [X] T087 Confirm `git status` shows no change under `services/api-gateway`, `services/identity-access`, or any other sibling service, and that `services/identity-access-next/email.txt` (if present) is untouched and uncommitted.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies. T003 depends on T002.
- **Foundational (Phase 2)**: Depends on Setup. Blocks every user story.
- **US1 (P1)** and **US2 (P1)**: Both depend only on Foundational. They share no production file; they share test fakes (`AuthenticationTestFakes.java`: T029 then T037) and the e2e file (`ThrottlingE2eTest.java`: T034 then T048).
- **US3 (P2)**: Depends on Foundational. Edits `RequestEmailChangeService.java` after US2 (T053 after T044) and `EmailChangeApplicationServiceTest.java` after T045.
- **US4 (P2)**: Depends on Foundational. Edits `RequestEmailVerificationService.java` after US3 (T068 after T052) and `RegisterUserUseCase`/service/tests independently. Its mail adapter work (T062 to T065) is independent of US1 to US3.
- **US5 (P3)**: Depends on Foundational (breaker wiring, Redis timeout). Independent of US1 to US4 apart from the shared e2e file (`ThrottlingE2eTest.java`: T077 after T056).
- **US6 (P3)**: Depends on T017 (breakers). Independent of the other stories; `application.yml` edit (T080) follows T027.
- **Polish (Phase 9)**: Depends on all desired stories.

### Within Each User Story

- Tests are written first and must fail before the implementation task (they do not compile or pass until then).
- Core before infrastructure before presentation before end-to-end.
- A task that edits a file already edited by an earlier task says so; do those in the stated order.

### Parallel Opportunities

- Phase 2: T004 to T009, T011, T012, T013, T016 and T023 are independent files and run together; T020 and T021 run once their classes exist.
- After Phase 2: US1, US2, and US5 and US6 can run in parallel with each other; US3 follows US2's `RequestEmailChangeService` change; US4's mail adapter tasks (T057 to T065) can run alongside US3.
- Inside a story, every `[P]` task touches a different file.

---

## Parallel Example: Foundational types

```bash
Task: "Create AttemptPolicy in CORE_MAIN/shared/throttle/AttemptPolicy.java"
Task: "Create AttemptOutcome in CORE_MAIN/shared/throttle/AttemptOutcome.java"
Task: "Create TooManyAttemptsException in CORE_MAIN/shared/exception/TooManyAttemptsException.java"
Task: "Create DependencyUnavailableException in CORE_MAIN/shared/exception/DependencyUnavailableException.java"
Task: "Create MailDeliveryException in CORE_MAIN/shared/exception/MailDeliveryException.java"
Task: "Create throttle-hit.lua in INFRA_RES/redis/throttle-hit.lua"
Task: "Add error codes in PRES_MAIN/shared/error/ErrorCode.java"
```

## Parallel Example: User Story 4

```bash
Task: "RecordingMailSender failing switch in START_TEST/e2e/RecordingMailSender.java"
Task: "MailDispatcherTest in INFRA_TEST/adapter/outbound/verification/mail/MailDispatcherTest.java"
Task: "RegisterUserService mail-failure test in CORE_TEST/user/internal/application/UserAccountApplicationServiceTest.java"
Task: "Resend mail-failure test in CORE_TEST/emailverification/internal/application/RequestEmailVerificationServiceTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (the limiter, breakers, error codes, configuration)
3. Complete Phase 3: User Story 1 (sign-in throttling)
4. **STOP and VALIDATE**: five wrong sign-ins then `429`, from different callers and for an unknown email, with the existing suite still green
5. Ship if ready: this closes the main brute-force path

### Incremental Delivery

1. Setup + Foundational: limiter and breakers exist, nothing uses them yet, no behavior change
2. Add US1: sign-in throttle (MVP)
3. Add US2: password confirmation throttle across five actions
4. Add US3: resend and email-change request limits
5. Add US4: mail breaker and graceful registration (the only story that changes the `register` response)
6. Add US5: bounded, fail-fast, fail-closed revocation lookup
7. Add US6: health visibility
8. Each step leaves the earlier ones working; run `./mvnw verify` at each checkpoint

### Notes

- [P] tasks = different files, no dependencies
- [Story] label maps each task to a user story for traceability
- The only intentional contract changes are those in `contracts/http-surface.md`: new `429` and `503 mail_unavailable` outcomes, the `verificationMailSent` field, and resend possibly answering `429`
- Commit after each task or logical group; use the repository's commit conventions
