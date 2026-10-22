# Implementation Plan: Abuse Throttling and Outage Resilience

**Branch**: `identity-access-next` | **Date**: 2026-10-19 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/007-abuse-throttling-resilience/spec.md`

## Summary

Add two kinds of protection to `identity-access-next`, both invisible to any request that stays under its limits.

1. **Throttling by business identity.** One new core port, `AttemptLimiter`, counts attempts per *subject* (a normalized email, a user id) in a fixed window and answers "allowed, or wait N seconds". One Redis adapter implements it with a single atomic Lua script. Five policies use it: sign-in per email, password confirmation per user (shared by change password, email-change request, close account, and the two password-confirmed session commands), resend per address (1-minute gap and per-hour cap), and email-change requests per user. Counting happens *before* the work (hit first, clear on success), so concurrent requests cannot overshoot a limit and nothing needs a read-then-write race. A blocked request returns a new `429 too_many_attempts` with `Retry-After`.
2. **Hold-off after repeated dependency failures.** Two circuit breakers from `resilience4j-circuitbreaker` (the plain library, no Spring Boot starter): one around mail delivery, one around the Redis calls on the request path (the revocation filter and the limiter). Mail keeps its existing retry, moved inside the breaker so one logical send is one breaker result. Registration now survives a mail failure (the account is committed and the response says the mail is delayed); an email-change request reports `503 mail_unavailable` and leaves the email unchanged; resend stays a blind `202`. Redis gets explicit short timeouts. Both breakers show in the health endpoint as a `DEGRADED` component.

The API gateway is untouched. Decisions: [research.md](research.md). Shapes: [data-model.md](data-model.md). Interfaces: [contracts/](contracts/). Validation: [quickstart.md](quickstart.md).

## Technical Context

**Language/Version**: Java 21, Spring Boot 4.0.8 / Spring Framework 7.0.9 (unchanged)

**Primary Dependencies**: One new library, `io.github.resilience4j:resilience4j-circuitbreaker:2.4.0` (core module only, used programmatically; its Spring Boot starter is deliberately not used, see research section 1). Reuses Spring Data Redis, Spring Framework's own `RetryTemplate`, `TokenDigest`, `RedisScripts`, `RedisKeyProperties`, Actuator

**Storage**: Redis, new key family `identity-access:throttle:{policy}:{sha256(subject)}` (integer counter, expires with its window). `throttle-hit.lua` is the only new script. PostgreSQL untouched, no migration

**Testing**: JUnit 5 + AssertJ; core service tests with an in-memory `AttemptLimiter` fake; Redis adapter contract test and breaker tests (Testcontainers, same `RedisTestSupport`); ArchUnit port map and structure rules; end-to-end suite in `identityaccess-start` for 429, mail outage, and store outage; Spotless `check`

**Target Platform**: JVM service, Linux/WSL development with the shared Docker stacks

**Project Type**: Multi-module web-service backend. Touches `identityaccess-core`, `identityaccess-infrastructure`, `identityaccess-presentation`, and `identityaccess-start` (configuration and tests)

**Performance Goals**: One extra Redis round trip on sign-in, on each password-confirmed action, on resend, and on email-change request. Nothing added on the signed-in read path (the revocation lookup already exists and only gains the breaker). While a dependency is held off, a request is answered in well under 2 seconds (SC-005, SC-006)

**Constraints**: Fail closed everywhere (FR-014, FR-020); no behavior change under the limits (FR-023); raw emails are never used in Redis keys, only their digest; no tokens, links, or passwords in logs; core imports no infrastructure, Redis, mail, or resilience type; no second `Duration` bean in the infrastructure module (existing rule in `SessionConfiguration`)

**Scale/Scope**: 1 new port, 1 new adapter, 1 new Lua script, 3 new core exceptions, 2 policy types, 1 core guard class; about 12 production classes edited; 1 new error code pair; 2 breakers, 2 health indicators; about 14 new or edited test classes

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is still an unratified template with no enforceable principles, so the active constraints are the repository `AGENTS.md`, the infrastructure structure rules in `StructureRules`, and the approved spec (same basis as specs 003 to 006).

| Gate | Before research | After design |
|------|-----------------|--------------|
| Only files relevant to the task change; sibling services untouched (`AGENTS.md`) | Pass | Pass: edits stay in `identity-access-next` and `specs/`. The gateway fixes stay out of scope |
| Core owns contracts; infrastructure owns technology | Pass | Pass: `AttemptLimiter` is a plain core interface; Redis, Lua, and resilience4j types live in infrastructure; the breaker's "call not permitted" is translated to a core exception inside the adapter |
| Every core port has exactly one adapter in the expected package (`StructureRules`) | Needs work | Pass: `PORT_MAP` gains `shared.port.AttemptLimiter` to `RedisAttemptLimiter` in `throttle`; the mail decorator idea was dropped so the two mail ports keep exactly one adapter each |
| Persistence records and mappers stay package-private | Pass | Pass: no record or mapper is added; the counter is a plain string value |
| No catch-all folders (`service`, `helper`, `config`, `persistence`) | Pass | Pass: new code lives in `adapter/outbound/throttle` and `integration/resilience` |
| Secrets, tokens, and passwords never logged or echoed | Pass | Pass: subjects are hashed in keys and never logged; any new log line carries only a breaker or policy name and an exception class, and must stay clear of the words `LoggingRuleTest` scans for (`password`, `hash`, `token`, `credential`, ...), so policy names in log statements use neutral names such as `reconfirmation` |
| No observable behavior change below the limits | Pass | Pass with two deliberate, documented additions: `RegisterResponse` gains a field, and resend may now answer `429` ([contracts/http-surface.md](contracts/http-surface.md)) |
| Fail closed when protective state is unreadable | Pass | Pass: limiter failure is `503`, never "unlimited"; the revocation filter keeps its existing `503` |
| Earlier specs stay accurate | Pass | Pass: spec 004's resend contract ("always 202") gets a one-line "amended by 007" note as a task |
| Git/patch paths against repository root, build commands against the workspace | Pass | Pass: quickstart states working directories and uses the per-service `./mvnw` |

No violations. Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/007-abuse-throttling-resilience/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── http-surface.md
│   ├── core-and-infra-changes.md
│   └── configuration.md
├── checklists/requirements.md
└── tasks.md             # created by /speckit-tasks, not by this command
```

### Source Code (repository root: `services/identity-access-next/`)

```text
pom.xml                                          # edited: resilience4j.version property + dependencyManagement entry

identityaccess-core/src/main/java/.../
├── shared/
│   ├── port/AttemptLimiter.java                 # new port: hit(policy, subject), clear(policy, subject)
│   ├── throttle/
│   │   ├── AttemptPolicy.java                   # new record: name, maxAttempts, window
│   │   ├── ThrottlePolicies.java                # new record: the five policies, provided as a bean by infrastructure
│   │   └── AttemptOutcome.java                  # new record: allowed, retryAfter
│   └── exception/
│       ├── TooManyAttemptsException.java        # new: carries retryAfter
│       ├── DependencyUnavailableException.java  # new: a protective store could not be used; carries retryAfter
│       └── MailDeliveryException.java           # new: a notifier could not deliver (thrown by both mail adapters)
├── authentication/internal/application/
│   ├── AccountVerifier.java                     # edited: goes through ReauthenticationGuard
│   └── command/
│       ├── AuthenticateService.java             # edited: hit sign-in policy before verifying, clear on success
│       ├── LogoutSessionService.java            # edited: wiring (guard)
│       └── LogoutOtherSessionsService.java      # edited: wiring (guard)
├── emailverification/internal/application/
│   └── RequestEmailVerificationService.java     # edited: gap policy, then hourly policy, per address; swallows MailDeliveryException
└── user/
    ├── api/command/RegisterUserUseCase.java     # edited: Registered gains verificationMailSent
    └── internal/application/
        ├── ReauthenticationGuard.java           # new: hit, run, clear unless the password was wrong
        └── command/
            ├── RegisterUserService.java         # edited: catch MailDeliveryException, still return Registered
            ├── ChangePasswordService.java       # edited: runs through the guard
            ├── RequestEmailChangeService.java   # edited: guard, then email-change-request policy before issuing
            └── SoftDeleteUserAccountService.java # edited: runs through the guard

identityaccess-core/src/test/java/.../
├── shared/throttle/InMemoryAttemptLimiter.java  # new fake (same contract as the Redis adapter)
├── authentication/internal/application/{AuthenticateServiceTest, LogoutSessionServiceTest, LogoutOtherSessionsServiceTest}.java   # edited
├── emailverification/internal/application/RequestEmailVerificationServiceTest.java   # edited
└── user/internal/application/
    ├── ReauthenticationGuardTest.java           # new
    └── {UserAccountApplicationServiceTest, EmailChangeApplicationServiceTest}.java    # edited

identityaccess-infrastructure/src/main/
├── java/.../
│   ├── adapter/outbound/throttle/
│   │   ├── RedisAttemptLimiter.java             # new: the one adapter of AttemptLimiter, runs inside the Redis breaker
│   │   ├── ThrottleProperties.java              # new: identity-access.throttle.* (validated)
│   │   └── ThrottleConfiguration.java           # new: exposes the ThrottlePolicies bean
│   ├── adapter/outbound/verification/mail/
│   │   ├── MailDispatcher.java                  # new, package-private: breaker( retry( send ) ), throws MailDeliveryException
│   │   ├── MailVerificationNotifier.java        # edited: builds the message, hands it to MailDispatcher; @Retryable removed
│   │   └── MailEmailChangeNotifier.java         # edited: same
│   ├── integration/mail/MailConfiguration.java  # DELETED (nothing left that needs @EnableResilientMethods)
│   ├── integration/resilience/
│   │   ├── ResilienceProperties.java            # new: identity-access.resilience.{mail,redis}.* (validated)
│   │   ├── ResilienceConfiguration.java         # new: CircuitBreakerRegistry and the two named breakers
│   │   └── DependencyHealthIndicators.java      # new: "mailDelivery" and "sessionStore" components, DEGRADED when not CLOSED
│   └── security/
│       ├── RevokedAccessTokenFilter.java        # edited: the hasKey call runs inside the Redis breaker
│       └── SecurityConfiguration.java           # edited: passes the breaker to the filter when present
└── resources/redis/throttle-hit.lua             # new: INCR, set expiry on first hit, return count and remaining ms

identityaccess-infrastructure/src/test/java/.../
├── adapter/outbound/throttle/RedisAttemptLimiterTest.java            # new: window, clear, atomic under concurrency, breaker open
├── adapter/outbound/throttle/ThrottlePropertiesTest.java             # new: defaults, invalid values stop startup
├── integration/resilience/ResiliencePropertiesTest.java              # new: same for the breaker settings
├── adapter/outbound/verification/mail/{MailVerificationNotifierTest, MailEmailChangeNotifierTest}.java   # edited
├── adapter/outbound/verification/mail/MailDispatcherTest.java        # new: retry inside breaker, open and recover
├── integration/resilience/DependencyHealthIndicatorsTest.java        # new
├── security/RevokedAccessTokenFilterTest.java                        # edited: breaker open is 503
└── structure/StructureRules.java                                     # edited: PORT_MAP row for AttemptLimiter

identityaccess-presentation/src/main/java/.../
├── shared/error/
│   ├── ErrorCode.java                           # edited: TOO_MANY_ATTEMPTS (429), MAIL_UNAVAILABLE (503)
│   ├── ApiErrors.java                           # edited: response(code, retryAfter) adds Retry-After
│   └── ApiExceptionHandler.java                 # edited: three new handlers
├── authentication/web/AuthController.java       # edited: @DocumentedErrors, register response
├── authentication/dto/RegisterResponse.java     # edited: verificationMailSent
├── verification/web/VerificationController.java # edited: @DocumentedErrors (429)
├── session/web/SessionController.java           # edited: @DocumentedErrors (429)
└── user/web/UserController.java                 # edited: @DocumentedErrors (429, 503 mail)

identityaccess-start/src/main/resources/application.yml   # edited: throttle, resilience, redis timeouts, health settings
identityaccess-start/src/test/java/.../e2e/
├── ThrottlingE2eTest.java                       # new: sign-in, confirmation, resend, email change
├── MailOutageE2eTest.java                       # new: failing sender, hold-off, recovery, registration still pending
├── RevocationStoreFailureE2eTest.java           # edited: bounded time and the Retry-After header
└── RecordingMailSender.java                     # edited: can be told to fail
```

**Structure Decision**: Counting lives behind one core port so every rule (what to count, for whom, how long) is stated in core services and every mechanism (Redis keys, Lua, timeouts, breaker) in infrastructure. A single `ReauthenticationGuard` is shared by the five password-confirmed actions so the "one count per person across all of them" rule (FR-006) cannot drift between services. The breaker wraps the *call*, not the port, inside the two adapters that make those calls, which avoids a second implementation of each notifier port and keeps `StructureRules` unchanged except for one new row. Resilience4j is used as a plain library because the hexagonal rules want it out of core and because its Boot 4 auto-configuration is not needed for two instances.

## Complexity Tracking

(empty: no constitution violations)
