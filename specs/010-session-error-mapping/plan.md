# Implementation Plan: Session Endpoint Error Causes

**Branch**: `identity-access-next` | **Date**: 2026-10-27 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/010-session-error-mapping/spec.md`

## Summary

Make core report why a session endpoint refused the caller, so presentation maps causes to HTTP answers without guessing. Three changes in core and one in presentation do this:

1. **One credential-confirmation rule in `UserAccount`.** A new `confirmCredentials` method and the existing change-password, change-email and close operations share one private rule: status first, then password format, then password match. The current password is passed in as the raw string so a malformed one is judged after the status check (spec FR-004).
2. **Two new core exceptions** replace the generic one on the session endpoints: `UserAccountNotFoundException` (account row missing) and `SessionUnusableException` (current session expired or not owned). A non-active account that exists raises the existing `InvalidAccountStateTransitionException`.
3. **`AccountVerifier`, `FindSessionsService` and `LogoutOtherSessionsService` stop throwing `AuthenticationFailedException`.** `ReauthenticationGuard` keeps the attempt count only for `InvalidCredentialsException`.
4. **Presentation deletes both wrapper exceptions and the three try/catch blocks**, maps the new exceptions in `ApiExceptionHandler`, and adds one error code, `session_unusable` (`401`).

`AuthenticationFailedException` is then thrown only by sign-in and renewal. No HTTP status changes except the new `401` for an unusable current session; on the other cases only the code changes (`reauthentication_failed` becomes `account_inaccessible` for a non-active or missing account on the two password-confirmed session endpoints). Decisions: [research.md](research.md). Shapes: [data-model.md](data-model.md). Interfaces: [contracts/](contracts/). Validation: [quickstart.md](quickstart.md).

## Technical Context

**Language/Version**: Java 21, Spring Boot 4.0.8 (Spring Framework 7, Spring Security 7)

**Primary Dependencies**: None added.

**Storage**: None. No schema, key, or script changes.

**Testing**: JUnit 5 + AssertJ; core unit tests (`UserAccountTest`, `ReauthenticationGuardTest`, the session service tests); presentation `@WebMvcTest`-style tests (`SessionControllerTest`, `ApiExceptionHandlerTest`, `ErrorProbeController`); end-to-end suite in `identityaccess-start` (Testcontainers `postgres:17`, `redis:7`); Spotless `check`.

**Target Platform**: JVM service, Linux/WSL development with the shared Docker stacks

**Project Type**: Multi-module web-service backend; touches `identityaccess-core` and `identityaccess-presentation`, plus tests in both and in `identityaccess-start`. `identityaccess-infrastructure` is not touched.

**Performance Goals**: No change. Same lookups, same password comparison count or fewer (a non-active account no longer reaches the hasher).

**Constraints**: Error bodies keep the single `ApiError` shape and fixed messages with no submitted values; no credential or password in any exception message; `AuthenticationFailedException` responses on sign-in and renewal stay byte-identical; the `reconfirmation` limit's size, window, and `429` answer stay unchanged.

**Scale/Scope**: 2 new core exception classes, 1 new error code, about 8 production classes edited in core and 5 in presentation, 2 presentation classes deleted, about 12 test classes edited, 6 documents updated.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is an unratified template with no enforceable principles, so the active constraints are the repository `AGENTS.md`, the approved spec, and the conventions of specs 003 to 009.

| Gate | Before research | After design |
|------|-----------------|--------------|
| Only files relevant to the task change; sibling services untouched (`AGENTS.md`) | Pass | Pass: edits are limited to `services/identity-access/{core,presentation,start}` and `specs/` |
| Core owns contracts; infrastructure owns technology | Pass | Pass: the new exceptions live in core `api/exception` packages; infrastructure is untouched |
| Presentation depends only on core's public contracts | Pass | Pass: presentation imports the two new exceptions from the `api.exception` packages, like the existing ones |
| Secrets, tokens, and passwords never logged or echoed | Pass | Pass: new exceptions have fixed messages and carry no password; the handler returns fixed messages |
| Anti-enumeration surfaces keep their behavior | Needs care | Pass: sign-in and renewal code paths are not edited; their tests are the regression guard (spec SC-004) |
| Earlier specs stay accurate | Needs work | Pass: 004 R4 and the contract wording in 004, 007 and 002 are updated as tasks (spec FR-013) |
| Build commands use the per-service wrapper from the workspace root (`AGENTS.md`, project memory) | Pass | Pass: quickstart uses `./mvnw` from `services/identity-access` |

No violations. Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/010-session-error-mapping/
├── spec.md
├── plan.md              # This file
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   ├── error-answers.md        # cause-to-answer table for every affected endpoint
│   └── core-changes.md         # exception, domain-method, and service-flow changes
├── checklists/requirements.md
└── tasks.md             # created by /speckit-tasks, not by this command
```

### Source Code (workspace: `services/identity-access/`)

```text
identityaccess-core/
├── src/main/java/.../user/api/exception/
│   └── UserAccountNotFoundException.java                      # new
├── src/main/java/.../authentication/api/exception/
│   ├── SessionUnusableException.java                          # new
│   └── AuthenticationFailedException.java                     # edit: Javadoc names sign-in and renewal as the only sources
├── src/main/java/.../user/internal/model/
│   ├── UserAccount.java                                       # edit: confirmCredentials, requireAccessible; raw-string current password in changePassword, authorizeEmailChange, close
│   └── PlainPassword.java                                     # edit: static presented(String) that raises InvalidCredentialsException
├── src/main/java/.../user/internal/application/
│   ├── ReauthenticationGuard.java                             # edit: keep the count only for InvalidCredentialsException
│   └── command/
│       ├── UserAccountCommandService.java                     # edit: remove presentedPassword (moved into the domain rule)
│       ├── ChangePasswordService.java                         # edit: pass the raw current password; a weak one is now a wrong password
│       ├── RequestEmailChangeService.java                     # edit: pass the raw current password
│       └── SoftDeleteUserAccountService.java                  # edit: pass the raw current password
├── src/main/java/.../authentication/internal/application/
│   ├── AccountVerifier.java                                   # edit: lookup, then account.confirmCredentials; throws UserAccountNotFoundException
│   ├── command/LogoutOtherSessionsService.java                # edit: SessionUnusableException instead of AuthenticationFailedException
│   └── query/FindSessionsService.java                         # edit: UserAccountNotFoundException; account.requireAccessible
└── src/test/java/.../
    ├── user/internal/model/UserAccountTest.java               # edit: rule order, weak password, raw-string signatures
    ├── user/internal/application/ReauthenticationGuardTest.java        # edit: count kept only for wrong password
    ├── user/internal/application/{UserAccountApplicationServiceTest,EmailChangeApplicationServiceTest}.java   # edit: signatures, weak current password
    └── authentication/internal/application/
        ├── AccountVerifier-covering tests: LogoutSessionServiceTest, LogoutOtherSessionsServiceTest   # edit: new exceptions
        ├── FindSessionsServiceTest.java                       # edit
        └── AuthenticateServiceTest.java, RefreshAccessTokenServiceTest.java    # unchanged assertions; run as the regression guard

identityaccess-presentation/
├── src/main/java/.../presentation/shared/error/
│   ├── ErrorCode.java                                         # edit: add SESSION_UNUSABLE (401)
│   ├── ApiExceptionHandler.java                               # edit: drop the reauthentication wrapper handler; map UserAccountNotFoundException and SessionUnusableException
│   ├── AccountInaccessibleException.java                      # delete
│   └── ReauthenticationFailedException.java                   # delete
├── src/main/java/.../presentation/session/web/SessionController.java   # edit: remove three try/catch blocks; update @DocumentedErrors
├── src/main/java/.../presentation/user/web/UserController.java         # no change expected (already documents both codes)
└── src/test/java/.../presentation/
    ├── session/web/SessionControllerTest.java                 # edit
    └── shared/error/{ApiExceptionHandlerTest,ErrorProbeController}.java   # edit

identityaccess-start/
└── src/test/java/.../start/e2e/
    ├── SessionManagementE2eTest.java                          # edit: locked account, wrong password, weak password, unusable session
    └── ThrottlingE2eTest.java                                 # edit: a blocked account does not consume the reconfirmation count

specs/
├── 004-presentation-api/research.md                           # edit: R4 marked superseded by 010
├── 004-presentation-api/contracts/{http-api.md,error-model.md}   # edit: rows for the session endpoints and the context-dependent note
├── 007-abuse-throttling-resilience/contracts/http-surface.md  # edit: paragraph under the throttling table
└── 002-authentication-sessions/contracts/commands.md          # edit: failure wording for the two password-confirmed session commands
```

**Structure Decision**: The rule for confirming a current password belongs to `UserAccount`, which already owns it for change-password and change-email. Moving the session commands onto that rule, not copying it into `AccountVerifier`, is what removes the guesswork. `AccountVerifier` stays because it is the one place the session commands combine the lookup with the shared attempt limit. Presentation keeps one job: map each core exception to one error code.

## Complexity Tracking

No violations.
