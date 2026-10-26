---

description: "Task list for session endpoint error causes"
---

# Tasks: Session Endpoint Error Causes

**Input**: Design documents from `specs/010-session-error-mapping/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/error-answers.md, contracts/core-changes.md, quickstart.md

**Tests**: Included. The spec's success criteria (SC-001 to SC-007) are stated as test outcomes and the quickstart commits to core, presentation, and end-to-end coverage. Within each story, write the tests first and see them fail before the implementation tasks.

**Organization**: Grouped by user story. Paths are relative to the repository root. The service lives in `services/identity-access/`; build commands use the per-service wrapper `./mvnw` from that directory, never bare `mvn`. `<core>` = `services/identity-access/identityaccess-core/src`, `<pres>` = `services/identity-access/identityaccess-presentation/src`, `<start>` = `services/identity-access/identityaccess-start/src`, and `<pkg>` = `com/minewaku/chatter/identityaccess`. `<e2e>` = `<start>/test/java/<pkg>/start/e2e`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1 = wrong password vs blocked account on end-session actions; US2 = session list reports the same cause; US3 = one rule for confirming the current password; US4 = sign-in and renewal stay generic; US5 = unusable current session has its own answer

## Phase 1: Setup

**Purpose**: Establish a green baseline before touching code.

- [X] T001 From `services/identity-access`, run `./mvnw verify` (all modules) and confirm it passes before any change, so later failures are attributable. Docker must be running (Testcontainers). Record any test that is already red.

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: The additive types and the one domain rule that every story relies on. The three signature changes in `UserAccount` break their callers, so the callers are updated in this phase too.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete. `identityaccess-core` does not compile between T006 and T010 because callers still pass the old argument types; that is expected.

- [X] T002 [P] Create `<core>/main/java/<pkg>/user/api/exception/UserAccountNotFoundException.java`: `public final class UserAccountNotFoundException extends RuntimeException` with a no-argument constructor calling `super("User account not found")`. It must not extend `IllegalArgumentException` (data-model.md, New exceptions). Mirror the style of `InvalidCredentialsException.java` in the same package.
- [X] T003 [P] Create `<core>/main/java/<pkg>/authentication/api/exception/SessionUnusableException.java`: `public final class SessionUnusableException extends RuntimeException`, no-argument constructor calling `super("Session is not usable")`. Mirror `AuthenticationFailedException.java` in the same package.
- [X] T004 [P] Add `SESSION_UNUSABLE(HttpStatus.UNAUTHORIZED, "The session is no longer usable.")` to `<pres>/main/java/<pkg>/presentation/shared/error/ErrorCode.java`, directly after `UNAUTHENTICATED`. The wire code `session_unusable` comes from the existing `code()` method.
- [X] T005 [P] In `<core>/main/java/<pkg>/user/internal/model/PlainPassword.java` add `public static PlainPassword presented(String raw)`: returns `new PlainPassword(raw)`, and converts the constructor's `IllegalArgumentException` into `new InvalidCredentialsException("Current password is incorrect")` (import from `user.api.exception`). Javadoc per `contracts/core-changes.md`: input that fails the policy cannot be the real password. The constructor is unchanged.
- [X] T006 In `<core>/main/java/<pkg>/user/internal/model/UserAccount.java` (after T005) apply `contracts/core-changes.md`: add `public void requireAccessible(String operation)` that throws `new InvalidAccountStateTransitionException(status, operation)` unless `isAccessible()`; change `verifyAccessibleCredentials` to `(String presentedPassword, PasswordHasher passwordHasher, String operation)` running `requireAccessible(operation)`, then `PlainPassword.presented(presentedPassword)`, then `passwordHasher.matches(..., passwordHash)` raising `InvalidCredentialsException("Current password is incorrect")`; add `public void confirmCredentials(String presentedPassword, PasswordHasher passwordHasher)` using the operation text `"confirm current password"`; change `changePassword`, `authorizeEmailChange` and `close` to take `String currentPassword` as their first parameter (the new password stays `PlainPassword`), with `close` skipping `requireAccessible` and otherwise using `PlainPassword.presented` then `matches`. Replace the inline accessibility checks in `changeUsername` (operation `"change username"`) and `applyVerifiedEmail` (operation `"confirm email change"`) with `requireAccessible`, keeping those texts.
- [X] T007 [P] Edit `<core>/main/java/<pkg>/user/internal/application/command/ChangePasswordService.java`: pass `command.currentPassword()` (raw string) to `account.changePassword`; keep `new PlainPassword(command.newPassword())` for the new password.
- [X] T008 [P] Edit `<core>/main/java/<pkg>/user/internal/application/command/RequestEmailChangeService.java`: pass `command.currentPassword()` (raw string) to `account.authorizeEmailChange`; remove the `presentedPassword(...)` call.
- [X] T009 [P] Edit `<core>/main/java/<pkg>/user/internal/application/command/SoftDeleteUserAccountService.java`: pass `command.currentPassword()` (raw string) to `account.close`; remove the `presentedPassword(...)` call.
- [X] T010 Edit `<core>/main/java/<pkg>/user/internal/application/command/UserAccountCommandService.java` (after T007 to T009): delete `presentedPassword` and any import left unused (`PlainPassword`, `InvalidCredentialsException`). `getAccount` stays unchanged (research decision 4).
- [X] T011 Update `<core>/test/java/<pkg>/user/internal/model/UserAccountTest.java`: adapt every call to `close`, `changePassword` and `authorizeEmailChange` to pass the raw current-password string (keep `PlainPassword` constants where they are used to hash or as a new password; add `String` counterparts for the presented value). Add tests from `data-model.md`: for `confirmCredentials`, `changePassword` and `authorizeEmailChange`, each non-active status (`PENDING_VERIFICATION`, `LOCKED`, `SUSPENDED`, `DELETED`) raises `InvalidAccountStateTransitionException` with the right password, a wrong password, and a password that fails the policy (assert the hasher is never called); an `ACTIVE` account raises `InvalidCredentialsException` for a wrong password and for a policy-failing one; an `ACTIVE` account with the right password does not throw. For `close`: any status with the right password succeeds, a wrong or policy-failing password raises `InvalidCredentialsException`. For `requireAccessible`: throws for each non-active status, not for `ACTIVE`.
- [X] T012 [P] Update `<core>/test/java/<pkg>/user/internal/application/UserAccountApplicationServiceTest.java`, `<core>/test/java/<pkg>/user/internal/application/EmailChangeApplicationServiceTest.java` and, if they reference the removed or changed methods, `<core>/test/java/<pkg>/user/internal/application/UserAccountTestFakes.java`, so they compile and pass with the raw-string signatures. Behavior assertions stay the same here; new cases come in US3.
- [X] T013 From `services/identity-access`, run `./mvnw -pl identityaccess-core -am verify` and confirm it passes.

**Checkpoint**: Core compiles and its tests pass. Presentation and the session services still use the old behavior.

---

## Phase 3: User Story 1 - A client can tell a wrong password from a blocked account (Priority: P1) 🎯 MVP

**Goal**: "End one session" and "end other sessions" answer `403 reauthentication_failed` for a wrong or policy-failing password on an active account, and `403 account_inaccessible` for a missing or non-active account whatever password was sent (spec FR-001 to FR-004, FR-006, FR-010).

**Independent Test**: With a signed-in caller, call both actions with a wrong password and with a policy-failing password (expect `reauthentication_failed`), then lock the account and call both with the right and a wrong password (expect `account_inaccessible`).

### Tests for User Story 1 (write first; they must fail before T019 to T021)

- [X] T014 [P] [US1] Update `<core>/test/java/<pkg>/authentication/internal/application/LogoutSessionServiceTest.java`: wrong password and policy-failing password raise `InvalidCredentialsException`; each non-active status raises `InvalidAccountStateTransitionException` with the right password and with a wrong one, and the password hasher is not called; a missing account raises `UserAccountNotFoundException`; none of these is `AuthenticationFailedException`; a success still returns the revoke result.
- [X] T015 [P] [US1] Update `<core>/test/java/<pkg>/authentication/internal/application/LogoutOtherSessionsServiceTest.java` the same way for the wrong-password, non-active, and missing-account cases (replace the existing `AuthenticationFailedException` assertions for those cases only; leave the unusable-session assertions for T037). Assert no other session is revoked in each failure.
- [X] T016 [P] [US1] Update `<pres>/test/java/<pkg>/presentation/session/web/SessionControllerTest.java` for `revoke` and `revoke-others`: a use case throwing `InvalidCredentialsException` gives `403 reauthentication_failed`; `InvalidAccountStateTransitionException` and `UserAccountNotFoundException` give `403 account_inaccessible`; remove assertions that depend on the controller translating `AuthenticationFailedException`.
- [X] T017 [P] [US1] In `<pres>/test/java/<pkg>/presentation/shared/error/ErrorProbeController.java` add a probe `GET /probe/user-account-not-found` throwing `new UserAccountNotFoundException()`, and in `ApiExceptionHandlerTest.java` assert it gives `403`, code `account_inaccessible`, the fixed message, and a body with only the standard fields and no exception text.
- [X] T018 [P] [US1] Extend `<e2e>/E2eSupport.java` with a protected helper that locks the account behind an access credential by autowiring `LockUserAccountUseCase` and calling its command with `Long.parseLong(claim(jwt, "sub"))`. Then in `<e2e>/SessionManagementE2eTest.java`: extend `aWrongPasswordIsForbiddenNotUnauthenticatedAndChangesNothing` with a password that fails the policy (still `403 reauthentication_failed`), and add a test that locks the account and checks `revoke` and `revoke-others` answer `403 account_inaccessible` with the right password and with a wrong one.

### Implementation for User Story 1

- [X] T019 [US1] Edit `<core>/main/java/<pkg>/authentication/internal/application/AccountVerifier.java` per `contracts/core-changes.md`: inside `guard.confirm(userId, ...)` run `userRepository.findById(new UserId(userId)).orElseThrow(UserAccountNotFoundException::new)`, then `account.confirmCredentials(password, passwordHasher)`, then return the account. Remove the `try/catch (IllegalArgumentException)` and every `AuthenticationFailedException` use; update the class Javadoc to say the rule lives in `UserAccount`.
- [X] T020 [US1] Edit `<pres>/main/java/<pkg>/presentation/shared/error/ApiExceptionHandler.java`: add `UserAccountNotFoundException` to the handler for `InvalidAccountStateTransitionException` and `AccountInaccessibleException` (keep the presentation class in the set until T027) returning `ErrorCode.ACCOUNT_INACCESSIBLE`.
- [X] T021 [US1] Edit `<pres>/main/java/<pkg>/presentation/session/web/SessionController.java`: in `revoke` and `revokeOthers` remove the `try/catch (AuthenticationFailedException)` blocks so the use case result is returned directly, and add `ErrorCode.ACCOUNT_INACCESSIBLE` to the `@DocumentedErrors` of both (list endpoint untouched until T026).
- [X] T022 [US1] From `services/identity-access`, run `./mvnw -pl identityaccess-core,identityaccess-presentation -am verify` and confirm US1 tests pass.

**Checkpoint**: The two password-confirmed session actions report wrong password and blocked account separately. The list endpoint and the wrappers are not touched yet.

---

## Phase 4: User Story 2 - Listing sessions reports the same cause (Priority: P1)

**Goal**: `GET /sessions` answers `403 account_inaccessible` for a missing or non-active account, and the two web-layer wrapper types are gone (spec FR-003, FR-006, FR-012).

**Independent Test**: Lock an account that has a valid access credential, then list sessions and expect `account_inaccessible`; list for an active account and expect the same list as today.

### Tests for User Story 2 (write first; they must fail before T025 to T027)

- [X] T023 [P] [US2] Update `<core>/test/java/<pkg>/authentication/internal/application/FindSessionsServiceTest.java`: a missing account raises `UserAccountNotFoundException`; each non-active status raises `InvalidAccountStateTransitionException`; an active account returns its views; none raises `AuthenticationFailedException`.
- [X] T024 [P] [US2] Update `<pres>/test/java/<pkg>/presentation/session/web/SessionControllerTest.java` for `list`: `InvalidAccountStateTransitionException` and `UserAccountNotFoundException` from the query give `403 account_inaccessible`; an active result is returned unchanged.
- [X] T025 [P] [US2] In `<e2e>/SessionManagementE2eTest.java` add a test that locks the account (T018 helper) and then calls `GET /sessions` with the still-valid access credential, expecting `403 account_inaccessible`; the existing active-account list test stays unchanged.

### Implementation for User Story 2

- [X] T026 [US2] Edit `<core>/main/java/<pkg>/authentication/internal/application/query/FindSessionsService.java`: `findById(new UserId(command.userId())).orElseThrow(UserAccountNotFoundException::new)`, then `account.requireAccessible("list sessions")`, then return `views.findByUserId(account.id())`. Remove the `filter(UserAccount::isAccessible)` and the `AuthenticationFailedException` import.
- [X] T027 [US2] Edit `<pres>/main/java/<pkg>/presentation/session/web/SessionController.java`: in `list` remove the `try/catch (AuthenticationFailedException)` so the mapped list is returned directly; drop now-unused imports. Then delete `<pres>/main/java/<pkg>/presentation/shared/error/AccountInaccessibleException.java` and `<pres>/main/java/<pkg>/presentation/shared/error/ReauthenticationFailedException.java`; in `ApiExceptionHandler.java` remove the `ReauthenticationFailedException` handler and the `AccountInaccessibleException` entry (leave `InvalidAccountStateTransitionException` and `UserAccountNotFoundException` mapping to `ACCOUNT_INACCESSIBLE`); remove the `/probe/reauthentication-failed` probe from `ErrorProbeController.java` and its case from `ApiExceptionHandlerTest.java`; remove any remaining reference found by `grep -rn "ReauthenticationFailedException\|AccountInaccessibleException" services/identity-access`.
- [X] T028 [US2] From `services/identity-access`, run `./mvnw -pl identityaccess-core,identityaccess-presentation -am verify` and confirm it passes and the grep in T027 finds nothing.

**Checkpoint**: All three session endpoints report the real cause; no controller translates exceptions.

---

## Phase 5: User Story 3 - One rule for confirming the current password (Priority: P1)

**Goal**: The five password-confirmed actions agree: status first, then password; only a wrong password keeps the shared attempt count; a policy-failing current password is a wrong password on every action, including change-password (spec FR-005, FR-008; research decisions 2 and 5).

**Independent Test**: For each of the five actions, use an active account with a wrong password and a blocked account with the right password; expect `reauthentication_failed` then `account_inaccessible` (close ignores status).

### Tests for User Story 3 (write first; they must fail before T033)

- [X] T029 [P] [US3] Update `<core>/test/java/<pkg>/user/internal/application/ReauthenticationGuardTest.java`: the count is kept for `InvalidCredentialsException` and cleared for `InvalidAccountStateTransitionException`, `UserAccountNotFoundException`, `SessionUnusableException`, a conflict raised after the password was accepted, and success; remove cases that assume `AuthenticationFailedException` keeps the count.
- [X] T030 [P] [US3] In `<core>/test/java/<pkg>/user/internal/application/UserAccountApplicationServiceTest.java` add: change-password with a current password that fails the policy raises `InvalidCredentialsException` (not `IllegalArgumentException`) and saves nothing; change-password on a locked account with a policy-failing current password raises `InvalidAccountStateTransitionException`. In `EmailChangeApplicationServiceTest.java` add the same pair for the email-change request. Assert the hasher is not called for the locked-account cases.
- [X] T031 [P] [US3] In `<e2e>/AccountManagementE2eTest.java` add: `PUT /users/@me/password` with a current password that fails the policy gives `403 reauthentication_failed`; after locking the account (T018 helper) `PUT /users/@me/password` and `POST /users/@me/email-change` with the right password give `403 account_inaccessible`, while `POST /users/@me/close` with the right password still succeeds.
- [X] T032 [P] [US3] In `<e2e>/ThrottlingE2eTest.java` add a test next to the existing reconfirmation tests (around lines 105 to 140): make one fewer wrong-password attempt than the configured reconfirmation limit, lock the account and call `revoke-others` with the right password (expect `403 account_inaccessible`, not `429`), reinstate it through `ReinstateUserAccountUseCase`, then make one wrong-password attempt and expect `403 reauthentication_failed`, not `429` (the blocked-account call cleared the count).

### Implementation for User Story 3

- [X] T033 [US3] Edit `<core>/main/java/<pkg>/user/internal/application/ReauthenticationGuard.java`: the first catch clause becomes `catch (InvalidCredentialsException wrongPassword) { throw wrongPassword; }`; remove the `AuthenticationFailedException` import; update the class Javadoc so only a wrong password keeps the count and every other outcome (success, a blocked or missing account, an unusable session, a conflict after acceptance) clears it.
- [X] T034 [US3] From `services/identity-access`, run `./mvnw -pl identityaccess-core,identityaccess-presentation -am verify` and `./mvnw -pl identityaccess-start -am verify -Dtest='AccountManagementE2eTest,ThrottlingE2eTest,SessionManagementE2eTest' -Dsurefire.failIfNoSpecifiedTests=false`; confirm they pass.

**Checkpoint**: One rule, one count behavior, for all five actions.

---

## Phase 6: User Story 4 - Sign-in and renewal stay generic (Priority: P2)

**Goal**: `AuthenticationFailedException` is thrown only by sign-in and renewal, and their answers do not change (spec FR-007, SC-004).

**Independent Test**: Run the unchanged sign-in and renewal tests and confirm they pass; search the main code and confirm the exception is thrown nowhere else.

- [X] T035 [P] [US4] Edit the Javadoc of `<core>/main/java/<pkg>/authentication/api/exception/AuthenticationFailedException.java` to: "Generic denial thrown by sign-in and renewal only; never reveals whether an account, password, or session exists." The class and message are unchanged.
- [X] T036 [P] [US4] In `<pres>/test/java/<pkg>/presentation/session/web/SessionControllerTest.java` add a test that, for every exception the three use cases can raise after this feature (`InvalidCredentialsException`, `InvalidAccountStateTransitionException`, `UserAccountNotFoundException`, `SessionUnusableException`), the response code is never `authentication_failed`. (`SessionUnusableException` is mapped in T041; keep the assertion in this task and run it after T041.)
- [X] T037 [US4] Confirm the regression guard: run `AuthenticateServiceTest`, `RefreshAccessTokenServiceTest` (core), `AuthControllerTest`, `TokenControllerTest` (presentation) and the sign-in and refresh cases in `<e2e>/SessionLifecycleE2eTest.java` and `<e2e>/ThrottlingE2eTest.java`. Their assertions must not have been edited by this feature; `git diff --stat` for those files must show no change, and they must pass. Then run `grep -rn "AuthenticationFailedException" services/identity-access/*/src/main --include=*.java` and confirm the only code that throws it is `AuthenticateService`, `RefreshAccessTokenService` and the class itself, with `ApiExceptionHandler` only mapping it.

**Checkpoint**: The anti-enumeration surfaces are unchanged and the generic exception is confined to them.

---

## Phase 7: User Story 5 - An unusable current session has its own answer (Priority: P3)

**Goal**: "End other sessions" answers `401 session_unusable` when the caller's current session is expired or not owned by the caller, revokes nothing, and reports a wrong password first (spec FR-009, User Story 5).

**Independent Test**: Call "end other sessions" with a valid access credential whose session has been expired in the store; expect `401 session_unusable` and every other session intact.

### Tests for User Story 5 (write first; they must fail before T041 to T043)

- [X] T038 [P] [US5] In `<core>/test/java/<pkg>/authentication/internal/application/LogoutOtherSessionsServiceTest.java` replace the remaining unusable-session assertions (currently `AuthenticationFailedException`, around lines 65 to 92): an expired session, a session of another user and an unknown session id each raise `SessionUnusableException` and `revokeOtherSessions` is not called; a wrong password with an unusable session raises `InvalidCredentialsException`; a blocked account with an unusable session raises `InvalidAccountStateTransitionException`.
- [X] T039 [P] [US5] In `<pres>/test/java/<pkg>/presentation/shared/error/ErrorProbeController.java` add `GET /probe/session-unusable` throwing `new SessionUnusableException()`; in `ApiExceptionHandlerTest.java` assert `401`, code `session_unusable`, message "The session is no longer usable.", and a body with only the standard fields. In `SessionControllerTest.java` assert `revoke-others` returns the same when its use case throws it.
- [X] T040 [P] [US5] In `<e2e>/SessionManagementE2eTest.java` add a test: sign in twice, expire the first session in Redis by setting its `expiresAt` to a past instant (read the key layout and field name from `<infra>/main/resources/redis/session-create.lua` and `RedisAuthenticationSessionRepositoryTest.java`; use the `redis` field from `E2eSupport`), then call `revoke-others` with the first session's access credential and the right password. Expect `401 session_unusable` and that the second session still gets `200` on `GET /sessions`. If the access credential is already rejected earlier with `401 unauthenticated` because the store entry is gone, move the expiry to the `expiresAt` field only and note it in the test comment.

### Implementation for User Story 5

- [X] T041 [US5] Edit `<core>/main/java/<pkg>/authentication/internal/application/command/LogoutOtherSessionsService.java`: the `orElseThrow` on the current-session lookup throws `SessionUnusableException::new`; remove the `AuthenticationFailedException` import. The password check (via `AccountVerifier`) still runs first.
- [X] T042 [US5] Edit `<pres>/main/java/<pkg>/presentation/shared/error/ApiExceptionHandler.java`: add `@ExceptionHandler(SessionUnusableException.class)` returning `body(ErrorCode.SESSION_UNUSABLE)`.
- [X] T043 [US5] Edit `<pres>/main/java/<pkg>/presentation/session/web/SessionController.java`: add `ErrorCode.SESSION_UNUSABLE` to the `@DocumentedErrors` of `revokeOthers` only.
- [X] T044 [US5] From `services/identity-access`, run `./mvnw verify` (all modules) and confirm it passes, including `OpenApiE2eTest`; if it asserts the documented error list for these endpoints, update it to include `account_inaccessible` on both password-confirmed session endpoints and `session_unusable` on `revoke-others`.

**Checkpoint**: All five stories pass; every cause on the three session endpoints has its own answer.

---

## Phase 8: Polish and cross-cutting concerns

**Purpose**: Documents that describe the old behavior (spec FR-013, SC-007) and final checks.

- [X] T045 [P] Edit `specs/004-presentation-api/research.md`: at R4 (lines 23 to 27) add a first line "**Superseded by [010](../010-session-error-mapping/spec.md)** for the three session endpoints; sign-in and renewal still follow this decision." Keep the original text below it as history.
- [X] T046 [P] Edit `specs/004-presentation-api/contracts/http-api.md`: replace the rows at lines 90 and 101 (`AuthenticationFailedException (wrong password, inaccessible account ...)` giving `403 reauthentication_failed`) with the rows from `contracts/error-answers.md` for `revoke` and `revoke-others` (wrong password `403 reauthentication_failed`; account missing or not active `403 account_inaccessible`; current session unusable on `revoke-others` `401 session_unusable`), and add a pointer to 010. Edit `specs/004-presentation-api/contracts/error-model.md`: update the `reauthentication_failed` entry at line 26, add an `account_inaccessible` source note and a `session_unusable` entry, and replace point 1 at line 41 ("`AuthenticationFailedException` is context-dependent") to say it is now thrown only by sign-in and renewal.
- [X] T047 [P] Edit `specs/007-abuse-throttling-resilience/contracts/http-surface.md`: replace the paragraph at line 27 (it cites `SessionController` lines and the `catch` that rethrows `ReauthenticationFailedException`) with one that says the two session commands call `AccountVerifier`, which now reports wrong password, blocked or missing account, and unusable session as distinct exceptions mapped directly (see spec 010); `TooManyAttemptsException` and `DependencyUnavailableException` are unaffected.
- [X] T048 [P] Edit `specs/002-authentication-sessions/contracts/commands.md` near line 46: for `LogoutOtherSessionsUseCase` and `LogoutSessionUseCase`, state the failure outcomes now raised (`InvalidCredentialsException`, `InvalidAccountStateTransitionException`, `UserAccountNotFoundException`, and for `LogoutOtherSessionsUseCase` `SessionUnusableException`) instead of the generic `AuthenticationFailedException`, with a note "amended by 010".
- [X] T049 From `services/identity-access`, run `./mvnw spotless:apply` then `./mvnw verify` (all modules, Docker running) and confirm both pass.
- [X] T050 Run the stale-reference search from `quickstart.md` section 5: `grep -rn "ReauthenticationFailedException\|AccountInaccessibleException" services/identity-access specs | grep -v "specs/010"`. Any remaining hit must be a historical note marked superseded by 010; fix any other.
- [X] T051 Walk `quickstart.md` sections 1 to 5 against the finished code and mark each expectation as met in the commit message or PR description; fix any that is not.

---

## Dependencies and execution order

```text
Phase 1 (T001)
   └─► Phase 2 (T002..T013)  ── T002,T003,T004,T005 parallel ─► T006 ─► T007,T008,T009 parallel ─► T010 ─► T011,T012 ─► T013
          │
          ├─► US1 (T014..T022) ──► US2 (T023..T028)         US2 needs US1's handler and controller edits (same two files)
          │         │
          │         └─► US3 (T029..T034)                     needs US1 (AccountVerifier delegates to the rule)
          │
          ├─► US4 (T035..T037)                               needs US1 and US2 done for the grep in T037; T035 can start after Phase 2
          │
          └─► US5 (T038..T044)                               needs US1 (AccountVerifier first, session check second) and T003, T004
                    └─► Phase 8 (T045..T051)                 T045..T048 can start any time after Phase 2; T049..T051 last
```

- **US1 before US2**: both edit `ApiExceptionHandler.java` and `SessionController.java`; US2 finishes the controller and deletes the wrappers.
- **US3 and US5** both touch `ReauthenticationGuardTest`/`LogoutOtherSessionsServiceTest` indirectly; run their tasks in the order listed.
- **US4** has no production edits except a Javadoc; its tests assert the confinement.

## Parallel execution examples

- **Phase 2**: T002, T003, T004 and T005 are four different files and can be done together; after T006, T007, T008 and T009 are three different files.
- **US1 tests**: T014, T015, T016, T017 and T018 are five different files and can be written together before any implementation.
- **US3 tests**: T029, T030, T031 and T032 are four different files.
- **Polish documents**: T045, T046, T047 and T048 are four different spec files.

## Implementation strategy

1. **MVP = Phases 1 and 2 plus US1**: after T022 the user-visible defect is fixed on the two password-confirmed session actions; the list endpoint and the wrappers still exist but the wrappers are no longer used by those two.
2. **Increment 2 = US2**: the list endpoint and the deletion of the wrappers, which completes "presentation does not guess".
3. **Increment 3 = US3, then US4**: the shared attempt-count behavior and the change-password policy fix, then the regression confirmation.
4. **Increment 4 = US5** and **Polish**: the new `401 session_unusable` answer and the documents.
5. Run `./mvnw verify` at every checkpoint (T013, T022, T028, T034, T044, T049), not only at the end.
6. **Behavior changes to mention in the PR**: a non-active or missing account on `revoke` and `revoke-others` now answers `account_inaccessible` instead of `reauthentication_failed`; an unusable current session on `revoke-others` now answers `401 session_unusable` instead of `403 reauthentication_failed`; a policy-failing current password on change-password now answers `403 reauthentication_failed` instead of `400 validation_failed`.
7. **Not in this feature** (spec Assumptions): whether locking an account should revoke its sessions.

---

## Implementation notes (recorded 2026-10-27)

Where the work differed from the task text:

- **T001, T044, T049 (`./mvnw verify`)**: a full `verify` ends in an error in `identityaccess-start/pom.xml`, which binds a goal `check` that `spring-boot-maven-plugin` does not have. This predates the feature (the baseline run failed the same way after all tests had passed). `./mvnw clean test` was used as the gate instead. A `spotless` plugin is not configured in any pom, so `spotless:apply` and `spotless:check` could not be run.
- **T012**: `UserAccountApplicationServiceTest`, `EmailChangeApplicationServiceTest` and `UserAccountTestFakes` compiled and passed unchanged against the raw-string signatures; only new cases were added (T030).
- **T015, T038**: besides the cases listed, `wrong_password_changes_zero_sessions` and `five_wrong_passwords_block_the_sixth_attempt_and_revoke_nothing` in `LogoutOtherSessionsServiceTest` asserted `AuthenticationFailedException` and were updated to `InvalidCredentialsException`; the same two tests in `LogoutSessionServiceTest` were updated in T014.
- **T029**: no `SessionUnusableException` case was added to `ReauthenticationGuardTest`. That exception is thrown after the guard has returned, so it never passes through it (analysis finding I1).
- **T032**: the account is returned to active with `UnlockUserAccountUseCase` (`E2eSupport.unlockAccountOf`), not `ReinstateUserAccountUseCase`, because reinstating applies only to a suspended account.
- **T036** is implemented in `SessionControllerTest` as a loop over the four exceptions the use cases can raise.
- **Known flaky test**: `RevocationStoreFailureE2eTest.afterRepeatedFailuresTheStoreIsHeldOffThenQuestionedAgainOnItsOwn` asserts a held-off request answers in under 500 ms. It failed once in a full-suite run on a loaded machine (7174 ms) and passes when run alone (32 s for the class). It does not touch the code changed here.
- **Spec wording not changed**: the four wording issues from the analysis (C1 close-account carve-out in FR-004/SC-005, C2 FR-011 "all five", I1 FR-008 wording, D1 plan counts) are not applied; the behavior in the code follows the contracts and tests.
