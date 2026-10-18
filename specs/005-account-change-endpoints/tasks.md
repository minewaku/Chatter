---

description: "Task list for the Account Change Endpoints"
---

# Tasks: Account Change Endpoints

**Input**: Design documents from `specs/005-account-change-endpoints/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: Included. FR-035 requires automated tests for every action, and the secret-handling and no-session-loss guarantees (FR-003, FR-032, SC-002, SC-008) can only be shown by tests. Each story writes its tests first and sees them fail (compile errors count) before implementing. No test reads a real mailbox: email-change tests use fakes and the in-memory `RecordingMailSender`.

**Organization**: Tasks are grouped by user story. Foundational work (Phase 2) blocks every story. The three stories edit some shared files (`UserAccount.java`, `UserController.java`, `UserAccountApplicationServiceTest.java`, `UserControllerTest.java`, `AccountManagementE2eTest.java`), so do them in order US1 → US2 → US3 rather than in parallel.

## Format: `- [ ] [TaskID] [P?] [Story?] Description with file path`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1 to US3, matching `spec.md`

## Path Conventions

All paths are relative to `services/identity-access-next/` unless they start with `specs/`. `<P>` stands for `com/minewaku/chatter/identityaccess`. Examples: `identityaccess-core/src/main/java/<P>/user/...`, `identityaccess-presentation/src/main/java/<P>/presentation/user/...`. Run Maven only through `./mvnw` in `services/identity-access-next/` (never a bare `mvn`). The end-to-end suite and the Redis contract tests need Docker.

---

## Phase 1: Setup

- [X] T001 Baseline: from `services/identity-access-next/` run `./mvnw verify` and record that core, infrastructure, presentation, and start build and pass before any change. Stop and report if the baseline is red. *Result: `./mvnw verify` fails in `identityaccess-start` before running anything because that module's `pom.xml` binds a nonexistent `spring-boot:check` goal (pre-existing, unrelated). `./mvnw test` is green for all four modules and is the command used for every checkpoint below, in place of `verify`.*

---

## Phase 2: Foundational (blocks all user stories)

**Purpose**: two small shared edits that more than one story needs.

- [X] T002 [P] Add two codes to `identityaccess-presentation/src/main/java/<P>/presentation/shared/error/ErrorCode.java`, right after `PASSWORD_UNCHANGED`: `USERNAME_UNCHANGED(HttpStatus.CONFLICT, "The username was not changed.")` and `EMAIL_UNCHANGED(HttpStatus.CONFLICT, "The email was not changed.")`. Wire values are the lowercase names (`username_unchanged`, `email_unchanged`) via the existing `code()`.
- [X] T003 [P] Add a second constructor `DuplicateUserAccountException(String message)` (calls `super(message)`) to `identityaccess-core/src/main/java/<P>/user/api/exception/DuplicateUserAccountException.java`. The existing `(String, Throwable)` constructor stays. `ApiExceptionHandler` already maps the type to `409 account_already_exists`; do not change it.

**Checkpoint**: `./mvnw -pl identityaccess-core,identityaccess-presentation -am verify` is still green.

---

## Phase 3: User Story 1 - Close My Account Only With My Password (Priority: P1) 🎯 MVP

**Goal**: Closing requires the current password; a wrong or missing password is refused without ending the session; the method moves to `POST /api/v1/users/@me/close`.

**Independent Test**: Sign in, close with a wrong password (`403 reauthentication_failed`, same access credential still works on `GET /sessions`), with no password (`400 validation_failed`), then with the right password (`204`, every session ended, sign-in blocked). `DELETE /users/@me` answers `404 not_found`.

### Tests for User Story 1 (write first, expect failures)

- [X] T004 [P] [US1] In `identityaccess-core/src/test/java/<P>/user/internal/model/UserAccountTest.java` add tests for `close(PlainPassword, PasswordHasher)`: correct password returns `true` and sets `DELETED` with placeholder email and username; wrong password throws `InvalidCredentialsException` and leaves status, email, and username untouched; an account that is `SUSPENDED`, `LOCKED`, or `PENDING_VERIFICATION` can be closed with the correct password; an already `DELETED` account throws `InvalidCredentialsException` (placeholder hash never matches). Keep the existing no-argument `softDelete()` tests.
- [X] T005 [P] [US1] In `identityaccess-core/src/test/java/<P>/user/internal/application/UserAccountApplicationServiceTest.java` change every `SoftDeleteUserAccountUseCase.Command(userId)` (around lines 81-82, 252-256, 293-294) to `Command(userId, CURRENT_PASSWORD)`, build `SoftDeleteUserAccountService` with the deterministic hasher, and add cases: wrong password throws `InvalidCredentialsException`, the account stays active, and the session-revocation coordinator is never called; a successful close calls it once.
- [X] T006 [P] [US1] In `identityaccess-presentation/src/test/java/<P>/presentation/user/web/UserControllerTest.java` replace the `DELETE` close tests (around lines 142-167) with `POST /api/v1/users/@me/close`: `204` and the command carries the credential's `userId` and the body password; missing, blank, and 129-character `currentPassword` give `400 validation_failed` with no interaction on the use case; wrong password (use case throws `InvalidCredentialsException`) gives `403 reauthentication_failed`; no credential gives `401`; `DELETE /api/v1/users/@me` gives `404 not_found`; the body is never echoed in any response.

### Implementation for User Story 1

- [X] T007 [US1] In `identityaccess-core/src/main/java/<P>/user/internal/model/UserAccount.java` add `public boolean close(PlainPassword currentPassword, PasswordHasher passwordHasher)`: throw `InvalidCredentialsException("Current password is incorrect")` when `!passwordHasher.matches(currentPassword, passwordHash)`, otherwise `return softDelete();`. Do not require `isAccessible()`. Leave `softDelete()` as is.
- [X] T008 [US1] Change `identityaccess-core/src/main/java/<P>/user/api/command/SoftDeleteUserAccountUseCase.java`: `record Command(long userId, String currentPassword) {}`; `Result(boolean changed)` unchanged.
- [X] T009 [US1] In `identityaccess-core/src/main/java/<P>/user/internal/application/command/SoftDeleteUserAccountService.java` inject `PasswordHasher` (constructor becomes `(UserAccountRepository, PasswordHasher, AuthenticationSessionRevocationCoordinator)`), call `account.close(new PlainPassword(command.currentPassword()), passwordHasher)`, and keep revoking all sessions only when the close changed something. Sessions must not be touched before the password check passes (FR-003).
- [X] T010 [P] [US1] Create `identityaccess-presentation/src/main/java/<P>/presentation/user/dto/CloseAccountRequest.java`: `record CloseAccountRequest(@NotBlank @Size(max = 128) @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String currentPassword)` with `toString()` returning `"CloseAccountRequest"`, like `ChangePasswordRequest`.
- [X] T011 [US1] In `identityaccess-presentation/src/main/java/<P>/presentation/user/web/UserController.java` replace the `@DeleteMapping close(...)` method with `@PostMapping("/close") ResponseEntity<Void> close(@Valid @RequestBody CloseAccountRequest request, @AuthenticationPrincipal Jwt jwt)` that calls `softDelete.handle(new SoftDeleteUserAccountUseCase.Command(caller.userId(), request.currentPassword()))` and returns `204`. `@Operation` summary "Close the caller's account (requires the current password)"; `@DocumentedErrors({VALIDATION_FAILED, MALFORMED_REQUEST, UNAUTHENTICATED, REAUTHENTICATION_FAILED, SERVICE_UNAVAILABLE})`. Remove the unused `DeleteMapping` import. Update the Javadoc: no longer "idempotent 204".
- [X] T012 [US1] In `identityaccess-start/src/test/java/<P>/start/e2e/AccountManagementE2eTest.java` update `closingTheAccountEndsItsSessionsAndBlocksSignIn` to `api().post("/users/@me/close", Map.of("currentPassword", PASSWORD), tokens[0])` and the repeat close to expect `401`; add tests: wrong password gives `403` with `code=reauthentication_failed`, then `GET /sessions` with the same access credential is `200` and the refresh credential still refreshes; missing password gives `400`; `DELETE /users/@me` gives `404`.
- [X] T013 [P] [US1] In `identityaccess-start/src/test/java/<P>/start/e2e/AllowListE2eTest.java` (line ~32) replace the `DELETE /users/@me` entry with `POST /users/@me/close` and body `{"currentPassword":"x"}`; in `identityaccess-start/src/test/java/<P>/start/e2e/OpenApiE2eTest.java` (line ~27) replace `"DELETE /api/v1/users/@me"` with `"POST /api/v1/users/@me/close"`.
- [X] T014 [US1] In `identityaccess-start/src/test/java/<P>/start/e2e/SecretLeakageE2eTest.java` add the close endpoint to the existing sentinel-password requests (mirror the `/users/@me/password` entries around lines 95-99) so a wrong password sentinel never appears in the response, logs, or error body.

**Checkpoint**: `./mvnw -pl identityaccess-core,identityaccess-presentation,identityaccess-start -am verify` is green; US1 is shippable alone.

---

## Phase 4: User Story 2 - Change My Username (Priority: P2)

**Goal**: A signed-in person changes their username with no password; unchanged and taken names are reported as conflicts.

**Independent Test**: Sign in, `PUT /users/@me/username` returns `204`; the old name can then be registered by another person; the same name again gives `409 username_unchanged`; another account's name gives `409 account_already_exists`; the session stays valid throughout.

### Tests for User Story 2 (write first, expect failures)

- [X] T015 [P] [US2] In `identityaccess-core/src/test/java/<P>/user/internal/model/UserAccountTest.java` replace the `changeUsername` tests (around lines 167, 206-224) with the no-password form `changeUsername(Username)`: new name returns `true` and updates `updatedAt`; same name returns `false`; `SUSPENDED`, `LOCKED`, `PENDING_VERIFICATION`, and `DELETED` accounts throw `InvalidAccountStateTransitionException`. Delete the wrong-password case.
- [X] T016 [P] [US2] In `identityaccess-core/src/test/java/<P>/user/internal/application/UserAccountApplicationServiceTest.java` change `ChangeUsernameUseCase.Command` uses (around lines 93-98, 135-141) to `Command(userId, "new_person")` and build `new ChangeUsernameService(repository)` with no hasher; keep the duplicate-name case (`person_42`) expecting `DuplicateUserAccountException`.
- [X] T017 [P] [US2] In `identityaccess-presentation/src/test/java/<P>/presentation/user/web/UserControllerTest.java` add `PUT /api/v1/users/@me/username` tests: `204` with `{"username":"new_name"}` and the command carries the credential's `userId`; `Result(false)` gives `409 username_unchanged`; blank and 65-character username give `400 validation_failed` with `errors[].field == "username"` and no submitted value in the body; `DuplicateUserAccountException` gives `409 account_already_exists`; `InvalidAccountStateTransitionException` gives `403 account_inaccessible`; no credential gives `401`.

### Implementation for User Story 2

- [X] T018 [US2] In `UserAccount.java` change `changeUsername` to `public boolean changeUsername(Username newUsername)`: throw `InvalidAccountStateTransitionException(status, "change username")` unless `isAccessible()`; return `false` when `username.equals(newUsername)`; otherwise set it, `markUpdated()`, return `true`.
- [X] T019 [US2] Change `identityaccess-core/src/main/java/<P>/user/api/command/ChangeUsernameUseCase.java`: `record Command(long userId, String newUsername) {}`.
- [X] T020 [US2] In `identityaccess-core/src/main/java/<P>/user/internal/application/command/ChangeUsernameService.java` drop the `PasswordHasher` field and constructor parameter (constructor becomes `(UserAccountRepository)`), and call `account.changeUsername(new Username(command.newUsername()))`. Uniqueness stays with the database constraint, which `JdbcUserAccountRepository.save` already maps to `DuplicateUserAccountException`.
- [X] T021 [P] [US2] Create `identityaccess-presentation/src/main/java/<P>/presentation/user/dto/ChangeUsernameRequest.java`: `record ChangeUsernameRequest(@NotBlank @Size(min = 1, max = 64) String username)` (same constraints as `RegisterRequest.username`) with `toString()` returning `"ChangeUsernameRequest"`.
- [X] T022 [US2] In `UserController.java` add `@PutMapping("/username") ResponseEntity<?> changeUsername(@Valid @RequestBody ChangeUsernameRequest request, @AuthenticationPrincipal Jwt jwt)`: inject `ChangeUsernameUseCase` through the constructor; return `204` when `Result.changed()`, otherwise `ApiErrors.response(ErrorCode.USERNAME_UNCHANGED)`. `@DocumentedErrors({VALIDATION_FAILED, MALFORMED_REQUEST, UNAUTHENTICATED, ACCOUNT_INACCESSIBLE, USERNAME_UNCHANGED, ACCOUNT_ALREADY_EXISTS, SERVICE_UNAVAILABLE})`. Update `UserControllerTest`'s `@MockitoBean` list for the new use case.
- [X] T023 [US2] In `AccountManagementE2eTest.java` add: change username `204` and the old username can then be used to register a new account (proves the change was stored); the same username again `409 username_unchanged`; another account's username `409 account_already_exists` without naming the holder; an invalid username (66 characters) `400` with field `username`; the caller's access and refresh credentials still work after the change (FR-033); no credential `401`.
- [X] T024 [P] [US2] Add `PUT /users/@me/username` (body `{"username":"x"}`) to `AllowListE2eTest.java` and `"PUT /api/v1/users/@me/username"` to the expected set in `OpenApiE2eTest.java`.

**Checkpoint**: `./mvnw -pl identityaccess-core,identityaccess-presentation,identityaccess-start -am verify` is green.

---

## Phase 5: User Story 3 - Change My Email Address (Priority: P2)

**Goal**: A signed-in person requests an email change with the current password; a verification mail goes to the new address only; the old address stays in effect until the token is confirmed; confirmation is public and token-only.

**Independent Test**: Sign in, `POST /users/@me/email-change` returns `202`; sign-in works with the old address and not the new; the token read from the captured mail confirms (`200 confirmed`); then sign-in works with the new address and not the old; a repeat confirm answers `already_confirmed`.

### Tests for User Story 3 (write first, expect failures)

- [X] T025 [P] [US3] Create `identityaccess-core/src/test/java/<P>/user/internal/model/PendingEmailChangeTest.java`: `issue` sets `createdAt = clock` and `expiresAt = createdAt + lifetime`; zero or negative lifetime throws `IllegalArgumentException`; blank raw token, null user id, or null email throws; `confirm(now)` returns `true` once and `false` when already confirmed or expired; `reconstitute` rejects `confirmedAt` outside the lifetime.
- [X] T026 [P] [US3] In `UserAccountTest.java` replace the `changeEmail` tests (around lines 170, 233-249) with: `authorizeEmailChange(password, newEmail, hasher)` returns `true` for a different email and `false` for the same email after normalization (`" PERSON@example.com "`), throws `InvalidCredentialsException` on a wrong password, throws `InvalidAccountStateTransitionException` when not accessible, and never changes the stored email; `applyVerifiedEmail(Email)` replaces the email and `updatedAt`, and throws `InvalidAccountStateTransitionException` when not accessible.
- [X] T027 [P] [US3] Add fakes to `identityaccess-core/src/test/java/<P>/user/internal/application/UserAccountTestFakes.java`: `InMemoryPendingEmailChangeRepository` (keyed by raw token, one per user, `discardForUser`, `findByPresentedToken`), `RecordingEmailChangeNotifier` (records `(email, token, expiresAt)`), and a sequential `UniqueStringIdGenerator`. Then create `identityaccess-core/src/test/java/<P>/user/internal/application/EmailChangeApplicationServiceTest.java` covering **request**: success stores one pending change, notifies exactly the new address once, leaves the account email unchanged, and the expiry is 24 hours after the clock; same email (normalized) returns `requested=false`, stores nothing, sends nothing; email held by another account throws `DuplicateUserAccountException`, stores nothing, sends nothing; wrong password throws `InvalidCredentialsException` and an existing pending change is untouched; a second request replaces the first (old token no longer found); non-active account throws `InvalidAccountStateTransitionException`. **confirm**: valid token applies the new email and keeps the record as confirmed; repeat returns `AlreadyConfirmed`; blank, unknown, and expired tokens return `InvalidOrExpired`; address taken in the meantime throws `DuplicateUserAccountException` and the pending change is discarded; account no longer accessible returns `InvalidOrExpired`; an account whose email already equals the pending one returns `AlreadyConfirmed`-equivalent without error (retry after a partial failure).
- [X] T028 [P] [US3] In `UserAccountApplicationServiceTest.java` remove the `ChangeEmailUseCase`/`ChangeEmailService` imports and cases (lines ~13, 26, 101-106, 147-156), and add a case that `SoftDeleteUserAccountService` (constructed with the new `PendingEmailChangeRepository` fake) discards the user's pending email change on a successful close and leaves it when the password is wrong.
- [X] T029 [P] [US3] Create `identityaccess-infrastructure/src/test/java/<P>/infrastructure/adapter/outbound/emailchange/write/redis/RedisPendingEmailChangeRepositoryTest.java` (copy the Testcontainers pattern of `RedisEmailVerificationRepositoryTest`): save then `findByPresentedToken`; only the SHA-256 digest appears in any key and the raw token appears in no key or value; keys use the `email-change:token:` and `email-change:user:` prefixes; saving a second change for the same user deletes the first; `discardForUser` removes an unconfirmed record; a confirmed record stays readable until its TTL; TTL equals the expiry; a token saved by `RedisEmailVerificationRepository` is not found here and the reverse (purpose isolation, FR-026).
- [X] T030 [P] [US3] Create `identityaccess-infrastructure/src/test/java/<P>/infrastructure/adapter/outbound/emailchange/write/redis/PendingEmailChangeRecordMapperTest.java`: model to record and back preserves user id, new email, times (microsecond precision), and `confirmedAt`; the raw token is never part of the record.
- [X] T031 [P] [US3] Create `identityaccess-infrastructure/src/test/java/<P>/infrastructure/adapter/outbound/verification/mail/MailEmailChangeNotifierTest.java` (mocked `JavaMailSender`, as in `MailVerificationNotifierTest`): the message is addressed only to the new address, from the configured sender, with the email-change subject; the body link starts with `{public-base-url}/api/v1/users/email-change/confirm?token=` and carries the URL-encoded token; a send failure is retried 3 times then surfaces a `VerificationMailException` whose message contains neither the token nor the link.
- [X] T032 [P] [US3] Extend `identityaccess-infrastructure/src/test/java/<P>/infrastructure/adapter/outbound/verification/mail/VerificationLinkFactoryTest.java` for `emailChangeLinkFor(rawToken)` (default and configured path, trailing slash on the base, encoded token), and add `identityaccess-infrastructure/src/test/java/<P>/infrastructure/security/SecurityPropertiesTest.java` (no security-properties test existed) asserting `DEFAULT_PUBLIC_ENDPOINTS` contains `/api/v*/users/email-change/confirm`.
- [X] T033 [P] [US3] Create `identityaccess-presentation/src/test/java/<P>/presentation/user/web/EmailChangeControllerTest.java` (MockMvc, same slice setup as `VerificationControllerTest`): `GET /api/v1/users/email-change/confirm?token=t` gives `200 {"status":"confirmed"}` or `already_confirmed` with `Cache-Control: no-store`; `InvalidOrExpired`, a missing token, and a blank token give `400 invalid_or_expired_token`; `DuplicateUserAccountException` gives `409 account_already_exists`; an invalid `Authorization` header is ignored.
- [X] T034 [P] [US3] In `UserControllerTest.java` add `POST /api/v1/users/@me/email-change` tests: `202` with no body and the command carries the credential's `userId`, the body `currentPassword`, and `newEmail`; `Result(false)` gives `409 email_unchanged`; malformed email, blank password, and 255-character email give `400 validation_failed` naming the field with no submitted value; wrong password gives `403 reauthentication_failed`; `DuplicateUserAccountException` gives `409 account_already_exists`; non-active account gives `403 account_inaccessible`; no credential `401`.

### Implementation for User Story 3: core

- [X] T035 [US3] Create `identityaccess-core/src/main/java/<P>/user/internal/model/PendingEmailChange.java`: final class with `rawToken`, `userId`, `newEmail`, `createdAt`, `expiresAt`, `confirmedAt`; factories `issue(rawToken, userId, newEmail, clock, lifetime)` and `reconstitute(...)`; `isExpired(now)`, `isConfirmed()`, `confirm(now)`; invariants exactly as in `EmailVerification` (expiry after creation; confirmed time inside the lifetime; positive lifetime). Same style (Lombok `@Getter`, accessor names `rawToken()`, `userId()`, `newEmail()`, ...).
- [X] T036 [P] [US3] Create the two ports: `identityaccess-core/src/main/java/<P>/user/internal/port/repository/PendingEmailChangeRepository.java` (`void save(PendingEmailChange)`, `Optional<PendingEmailChange> findByPresentedToken(String token)`, `void discardForUser(UserId userId)`) and `identityaccess-core/src/main/java/<P>/user/internal/port/EmailChangeNotifier.java` (`void sendEmailChange(Email newEmail, String rawToken, Instant expiresAt)`).
- [X] T037 [US3] In `UserAccount.java` remove `changeEmail` and add `public boolean authorizeEmailChange(PlainPassword currentPassword, Email newEmail, PasswordHasher passwordHasher)` (calls `verifyAccessibleCredentials(..., "change email")`, returns `!email.equals(newEmail)`, never changes state) and `public void applyVerifiedEmail(Email newEmail)` (throws `InvalidAccountStateTransitionException(status, "confirm email change")` unless accessible; sets `email`; `markUpdated()`).
- [X] T038 [US3] Delete `identityaccess-core/src/main/java/<P>/user/api/command/ChangeEmailUseCase.java` and `.../user/internal/application/command/ChangeEmailService.java`. Create `identityaccess-core/src/main/java/<P>/user/api/command/RequestEmailChangeUseCase.java` (`Command(long userId, String currentPassword, String newEmail)`, `Result(boolean requested)`, extends `UseCaseHandler`) and `.../ConfirmEmailChangeUseCase.java` (`Command(String token)`, `sealed interface Result permits Confirmed, AlreadyConfirmed, InvalidOrExpired`, `record Confirmed(long userId)`, empty records for the other two).
- [X] T039 [US3] Create `identityaccess-core/src/main/java/<P>/user/internal/application/command/RequestEmailChangeService.java` (`@Service`, extends `UserAccountCommandService`; dependencies `UserAccountRepository`, `PasswordHasher`, `PendingEmailChangeRepository`, `EmailChangeNotifier`, `Clock`, `UniqueStringIdGenerator`; lifetime constant `Duration.ofHours(24)`). Order, exactly: load account; `authorizeEmailChange` (propagates wrong password and non-active); if it returns `false` return `Result(false)`; if `repository.findByEmail(newEmail)` is present throw `new DuplicateUserAccountException("User account already exists")`; `discardForUser`; issue and `save` the pending change; `notifier.sendEmailChange(newEmail, token, expiresAt)`; return `Result(true)`. Never log or put the token in an exception.
- [X] T040 [US3] Create `identityaccess-core/src/main/java/<P>/user/internal/application/command/ConfirmEmailChangeService.java` (`@Service`, `@Transactional`): blank or null token, unknown token, expired token → `InvalidOrExpired`; confirmed record → `AlreadyConfirmed`; load the account (missing → `InvalidOrExpired`); not accessible → `InvalidOrExpired`; account email already equals the pending email → mark confirmed, save the record, return `AlreadyConfirmed`; another account holds the new email (`findByEmail` present and a different id) → `discardForUser` then throw `DuplicateUserAccountException`; otherwise `applyVerifiedEmail`, `repository.save` (on `DuplicateUserAccountException` from the unique constraint call `discardForUser` and rethrow), then `confirm(now)`, save the record, return `Confirmed(userId)`.
- [X] T041 [US3] In `SoftDeleteUserAccountService.java` also inject `PendingEmailChangeRepository` and call `discardForUser(account.id())` after a successful close (FR-027), next to the session revocation.

### Implementation for User Story 3: infrastructure

- [X] T042 [P] [US3] Create `identityaccess-infrastructure/src/main/java/<P>/infrastructure/adapter/outbound/emailchange/write/redis/PendingEmailChangeRecord.java` and `PendingEmailChangeRecordMapper.java`, both package-private (the structure rules require it): record `(long userId, String email, long createdAt, long expiresAt, Long confirmedAt)` with `toHash()`/`fromHash()` using hash fields `userId`, `email`, `createdAt`, `expiresAt`, and `verifiedAt` for the confirmed time (so the existing invalidate script, which tests `HEXISTS verifiedAt`, keeps confirmed records). Times are epoch microseconds, like `VerificationRecord`. The raw token is never part of it.
- [X] T043 [US3] Create `identityaccess-infrastructure/src/main/java/<P>/infrastructure/adapter/outbound/emailchange/write/redis/RedisPendingEmailChangeRepository.java` (`@Repository`, implements `PendingEmailChangeRepository`): copy the structure of `RedisEmailVerificationRepository`, reuse `verification-save.lua` and `verification-invalidate-for-user.lua` unchanged, key prefixes `keys.keyPrefix() + "email-change:token:"` and `"email-change:user:"`, digest with `TokenDigest.sha256Hex`, `discardForUser` passes the token key prefix as the script argument.
- [X] T044 [US3] Extend `identityaccess-infrastructure/src/main/java/<P>/infrastructure/adapter/outbound/verification/mail/VerificationMailProperties.java` with `emailChangeSubject` (default `"Confirm your new Chatter email"`) and `emailChangeConfirmPath` (default `/api/v1/users/email-change/confirm`), and add `emailChangeLinkFor(String rawToken)` to `VerificationLinkFactory.java` (same base, same encoding). Update every call site of the properties constructor (production and tests).
- [X] T045 [P] [US3] Create `identityaccess-infrastructure/src/main/resources/templates/email-change.html` modelled on `email-verification.html`: copy says "Confirm your new Chatter email address" and "If you did not ask to change your Chatter email, ignore this message and your account stays as it is", uses `th:href="${link}"` and `th:text="${expiresAt}"`, and mentions neither the old address nor any password.
- [X] T046 [US3] Create `identityaccess-infrastructure/src/main/java/<P>/infrastructure/adapter/outbound/verification/mail/MailEmailChangeNotifier.java` (placed beside `MailVerificationNotifier` because `VerificationMailException` is package-private and the two share the link factory and properties; `@Component`, implements `EmailChangeNotifier`): same collaborators, `@Retryable(includes = VerificationMailException.class, maxRetries = 3, delay = 200, multiplier = 2)`, template `email-change`, recipient is the new address only, subject from `emailChangeSubject`, link from `emailChangeLinkFor`; failures throw `VerificationMailException` without the token or link.
- [X] T047 [US3] In `identityaccess-infrastructure/src/main/java/<P>/infrastructure/security/SecurityProperties.java` add `"/api/v*/users/email-change/confirm"` to `DEFAULT_PUBLIC_ENDPOINTS`. In `identityaccess-infrastructure/src/test/java/<P>/infrastructure/structure/StructureRules.java` add two port rows: `row("user.internal.port.repository.PendingEmailChangeRepository", "RedisPendingEmailChangeRepository", "emailchange.write.redis")` and `row("user.internal.port.EmailChangeNotifier", "MailEmailChangeNotifier", "verification.mail")`.
- [X] T048 [US3] In `identityaccess-start/src/main/resources/application.yml` add `- /api/v*/users/email-change/confirm` to `identity-access.security.public-endpoints` and, under `identity-access.verification.mail`, `email-change-subject: Confirm your new Chatter email` and `email-change-confirm-path: /api/v1/users/email-change/confirm`.

### Implementation for User Story 3: presentation

- [X] T049 [P] [US3] Create `identityaccess-presentation/src/main/java/<P>/presentation/user/dto/ChangeEmailRequest.java`: `record ChangeEmailRequest(@NotBlank @Email @Size(max = 254) String newEmail, @NotBlank @Size(max = 128) @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String currentPassword)` with `toString()` returning `"ChangeEmailRequest"`; and `identityaccess-presentation/src/main/java/<P>/presentation/user/dto/EmailChangeStatusResponse.java` with static factories `confirmed()` and `alreadyConfirmed()` giving `status` `"confirmed"` / `"already_confirmed"` (mirror `VerificationStatusResponse`).
- [X] T050 [US3] In `UserController.java` add `@PostMapping("/email-change") ResponseEntity<?> requestEmailChange(@Valid @RequestBody ChangeEmailRequest request, @AuthenticationPrincipal Jwt jwt)`: inject `RequestEmailChangeUseCase`; `202 Accepted` with no body when `Result.requested()`, otherwise `ApiErrors.response(ErrorCode.EMAIL_UNCHANGED)`. `@DocumentedErrors({VALIDATION_FAILED, MALFORMED_REQUEST, UNAUTHENTICATED, REAUTHENTICATION_FAILED, ACCOUNT_INACCESSIBLE, EMAIL_UNCHANGED, ACCOUNT_ALREADY_EXISTS, SERVICE_UNAVAILABLE})`. Update `UserControllerTest`'s `@MockitoBean` list.
- [X] T051 [US3] Create `identityaccess-presentation/src/main/java/<P>/presentation/user/web/EmailChangeController.java`: `@RestController @RequestMapping("/api/v1/users/email-change")`, tag "Account" with no `@SecurityRequirement`, `@GetMapping("/confirm")` with `@RequestParam(name = "token", required = false) String token`; blank token → `ApiErrors.response(INVALID_OR_EXPIRED_TOKEN)`; `switch` over the three results exactly like `VerificationController.confirm`, `Cache-Control: no-store` on success; `@DocumentedErrors({INVALID_OR_EXPIRED_TOKEN, ACCOUNT_ALREADY_EXISTS})`. The class lives in `user.web`, so the existing ArchUnit rule `..presentation.*.web..` already covers it.

### End-to-end for User Story 3

- [X] T052 [US3] Create `identityaccess-start/src/test/java/<P>/start/e2e/EmailChangeE2eTest.java` (extends `E2eSupport`, reads tokens with `RecordingMailSender.latestTokenFor`): request `202`, one mail to the new address and none to the old, sign-in with the old address works and with the new fails; confirm `200 confirmed`, repeat `200 already_confirmed`, then sign-in works with the new address and fails with the old; a second request replaces the first (first token gives `400 invalid_or_expired_token`, second confirms); a wrong password gives `403`, sends nothing, and the same access credential still works; same email gives `409 email_unchanged` and sends nothing; an address held by another account gives `409 account_already_exists` and sends nothing; the address taken by a new registration before confirming gives `409` on confirm and the old address still signs in; a registration verification token presented to the email-change confirm endpoint (and the reverse) gives `400`; closing the account discards a pending change (its token then gives `400`); the confirm endpoint works with an invalid `Authorization` header; the caller's refresh credential still works after confirmation (FR-033).
- [X] T053 [P] [US3] Add `POST /users/@me/email-change` (body `{"newEmail":"a@b.co","currentPassword":"x"}`) to `AllowListE2eTest.java`, add `"POST /api/v1/users/@me/email-change"` and `"GET /api/v1/users/email-change/confirm"` to `OpenApiE2eTest.java`, and add the new endpoint with a sentinel password to `SecretLeakageE2eTest.java`; also assert in that test that the mail-captured token and the pending email address never appear in any response body or captured log line other than the mail itself (SC-008, FR-032).

**Checkpoint**: `./mvnw verify` from `services/identity-access-next/` is green.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T054 [P] Correct spec 004 where this feature supersedes it: in `specs/004-presentation-api/spec.md` update FR-016 and the endpoint list (around line 143-146) so close is `POST /api/v1/users/@me/close` with `{"currentPassword": "..."}`; in `specs/004-presentation-api/contracts/http-api.md` replace the `DELETE /users/@me` row and its error list; in `specs/004-presentation-api/quickstart.md` update any close step. Add one line to each pointing at `specs/005-account-change-endpoints/`.
- [X] T055 [P] Re-read `specs/005-account-change-endpoints/contracts/http-api.md` and `quickstart.md` against the built behavior and fix any difference (status codes, codes, paths). In particular confirm that `DELETE /api/v1/users/@me` really answers `404 not_found` and correct both documents if it answers otherwise.
- [X] T056 Run `./mvnw spotless:apply` then `./mvnw verify` from `services/identity-access-next/` (Docker running). Everything must be green, including `StructureRules`, `PresentationArchitectureTest`, `SecretHandlingTest`, and the end-to-end suite.
- [ ] T057 Walk through [quickstart.md](quickstart.md) "Manual walkthrough" against a locally running service if the Docker stacks are available, and report any step whose result differs from the expected status. Skip and say so if they are not. *Not run: no live service was started. The same flows (wrong password keeps the session, username, email request and confirm, replacement, conflict, `DELETE /users/@me` giving `404`) are covered by the Testcontainers end-to-end suite, which passes. T056 used `spotless:check` plus `./mvnw test`, because `./mvnw verify` still fails on the unrelated `spring-boot:check` goal in `identityaccess-start/pom.xml` (see T001).*

**Not a task for the assistant**: the "Real delivery" step in the quickstart (a real inbox receiving a real message through the owner's SMTP account) is checked by the service owner by hand. No test or task reads a mailbox.

---

## Dependencies & Execution Order

- **Phase 1 → Phase 2 → stories**: T001 first; T002 and T003 can run together; stories need both.
- **User stories**: US1 (P1) first as the MVP. US2 and US3 each depend only on Phase 2, but all three edit `UserAccount.java`, `UserController.java`, `UserAccountApplicationServiceTest.java`, `UserControllerTest.java`, and `AccountManagementE2eTest.java`, so run them in order US1 → US2 → US3 unless the work is split across branches.
- **Within a story**: tests first (expected to fail or not compile), then core, then infrastructure, then presentation, then end-to-end.
- **US3 order**: T035 before T036-T041; T037 before T039 and T040; T042 before T043; T044 before T046; T049 before T050 and T051; T052 last.
- **Polish**: T054 and T055 any time after US3; T056 after all code tasks; T057 last.

## Parallel Examples

```text
Phase 2:   T002 | T003
US1 tests: T004 | T005 | T006
US2 tests: T015 | T016 | T017
US3 tests: T025 | T026 | T027 | T028 | T029 | T030 | T031 | T032 | T033 | T034
US3 infra: T042 | T045   then T043 | T044   then T046
Polish:    T054 | T055
```

## Implementation Strategy

- **MVP**: Phase 1, Phase 2, and User Story 1 (T001-T014). That closes the security gap in the existing close action and can ship on its own.
- **Increment 2**: User Story 2 (T015-T024), a small, low-risk change.
- **Increment 3**: User Story 3 (T025-T053), the only one that adds stored state and mail. Each increment ends with its checkpoint verify, so a broken build never carries over.
