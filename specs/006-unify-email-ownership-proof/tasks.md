---

description: "Task list for unifying the email ownership proof"
---

# Tasks: Unify Email Ownership Proof

**Input**: Design documents from `specs/006-unify-email-ownership-proof/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ (`core-and-infra-changes.md`, `unchanged-surface.md`), quickstart.md

**Tests**: Included. Spec FR-018 requires the shared mechanism to have at least the coverage the two copies had together, and SC-003 requires the existing registration and email-change tests to keep passing.

**Organization**: This is a behavior-preserving restructuring, so the shared mechanism itself (the purpose, the merged port, the issuer, the merged Redis adapter) is built first in the Foundational phase while the old `PendingEmailChange` still exists and everything compiles. User Story 1 then moves email change onto it and deletes the duplicate. User Stories 2 and 3 add the tests that pin purpose separation and lifecycle cleanup; the behavior they verify is delivered by Phases 2 and 3.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Repository root: `/home/minewaku/projects/java/2026/Chatter-identity-access-next`. Build commands run from `services/identity-access-next` with the per-service `./mvnw`, never a bare `mvn`. Git paths are relative to the repository root.

Path shorthands used below (all relative to the repository root):

- `CORE_MAIN` = `services/identity-access-next/identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess`
- `CORE_TEST` = `services/identity-access-next/identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess`
- `INFRA_MAIN` = `services/identity-access-next/identityaccess-infrastructure/src/main/java/com/minewaku/chatter/identityaccess/infrastructure/adapter/outbound`
- `INFRA_TEST` = `services/identity-access-next/identityaccess-infrastructure/src/test/java/com/minewaku/chatter/identityaccess/infrastructure`

---

## Phase 1: Setup

**Purpose**: Establish that the starting point is green, so any later failure is caused by this work.

- [X] T001 From `services/identity-access-next`, confirm Docker is up (`docker ps`) and run `./mvnw verify`; record that all modules pass before any edit. The Redis adapter tests use Testcontainers and the e2e suite in `identityaccess-start` needs the same stack.
- [X] T002 [P] Confirm the working tree has no uncommitted source changes under `services/identity-access-next` (`git status --short -- services/identity-access-next`); the account-change work is committed as `4707bfb`. The untracked `services/identity-access-next/email.txt` is the owner's credentials file and must stay untouched and uncommitted.

---

## Phase 2: Foundational (the shared mechanism)

**Purpose**: Add the purpose to the proof and make the issuer, the port, and the Redis adapter purpose-aware, with registration wired through them. `PendingEmailChange` and its adapter stay in place and keep compiling until Phase 3.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Core production code

- [X] T003 [P] Create the enum `VerificationPurpose` with values `REGISTRATION` and `EMAIL_CHANGE` in `CORE_MAIN/emailverification/internal/model/VerificationPurpose.java`.
- [X] T004 Edit `CORE_MAIN/emailverification/internal/model/EmailVerification.java`: add a `purpose` field, required (`Objects.requireNonNull(purpose, "purpose is required")`), exposed through the existing accessor style (`purpose()`). Change both factories to take it first: `issue(VerificationPurpose purpose, String rawToken, UserId userId, Email email, Clock clock, Duration lifetime)` and `reconstitute(VerificationPurpose purpose, String rawToken, UserId userId, Email email, Instant createdAt, Instant expiresAt, Instant verifiedAt)`. Keep every existing rule verbatim: `rawToken` not blank, `expiresAt` strictly after `createdAt`, `verifiedAt` within `[createdAt, expiresAt)`, `isExpired` true at and after `expiresAt`, `verify(now)` returns false when already verified or expired. Depends on T003.
- [X] T005 Edit the port `CORE_MAIN/emailverification/internal/port/repository/EmailVerificationRepository.java` to: `void save(EmailVerification verification)`, `Optional<EmailVerification> findByPresentedToken(VerificationPurpose purpose, String token)`, `void invalidateForUser(VerificationPurpose purpose, UserId userId)`. Add Javadoc that a token saved under one purpose is never returned for another, invalidating one purpose leaves the other's proof for the same user, and a verified proof is kept. Depends on T004.
- [X] T006 Create `CORE_MAIN/emailverification/internal/application/EmailVerificationIssuer.java`: a `@Service` with `public static final Duration LIFETIME = Duration.ofHours(24)`, a constructor `(Clock, UniqueStringIdGenerator, EmailVerificationRepository)` and a second constructor taking a `Duration` for tests (reject a zero or negative lifetime through `EmailVerification.requirePositive`, as the coordinator does now), and `EmailVerification issue(VerificationPurpose purpose, UserId userId, Email email)` that runs in this fixed order: `invalidateForUser(purpose, userId)`, `tokenGenerator.nextId()`, `EmailVerification.issue(...)`, `save`, return the proof. It sends no mail. Depends on T004, T005.
- [X] T007 Edit `CORE_MAIN/emailverification/internal/application/EmailVerificationIssuanceCoordinator.java`: remove `DEFAULT_LIFETIME` and both existing constructors; new constructor `(EmailVerificationIssuer issuer, VerificationNotifier notifier)` (keep `@Autowired` on it). Keep `issue(UserId userId, Email email)`: call `issuer.issue(REGISTRATION, userId, email)`, then `notifier.sendVerification(email, verification.rawToken(), verification.expiresAt())`. Depends on T006.
- [X] T008 [P] Edit `CORE_MAIN/emailverification/internal/application/VerifyEmailService.java`: call `verificationRepository.findByPresentedToken(VerificationPurpose.REGISTRATION, command.token())`. Change nothing else, including the order of checks and the result types. `RegisterUserService.java` and `RequestEmailVerificationService.java` need no edit because they only call `coordinator.issue(userId, email)`. Depends on T005.

### Infrastructure production code

- [X] T009 Edit `INFRA_MAIN/verification/write/redis/VerificationRecordMapper.java`: `toModel(VerificationRecord record, VerificationPurpose purpose, String presentedToken)` passes the purpose to `EmailVerification.reconstitute`. `toRecord` stays the same; `VerificationRecord.java` is not edited and stays package-private. Depends on T004.
- [X] T010 Edit `INFRA_MAIN/verification/write/redis/RedisEmailVerificationRepository.java`: replace the two fixed prefixes with a private method that returns the token and user key prefixes from the purpose using an exhaustive `switch` with no `default` branch: `REGISTRATION` uses `keys.keyPrefix() + "verification:token:"` and `"verification:user:"`, `EMAIL_CHANGE` uses `"email-change:token:"` and `"email-change:user:"`. `save` takes the purpose from the proof; `findByPresentedToken(purpose, token)` and `invalidateForUser(purpose, userId)` use it, and `invalidateForUser` passes that purpose's token prefix as the Lua script argument. Keep the blank-token short circuit. Do not touch `verification-save.lua` or `verification-invalidate-for-user.lua`. Depends on T005, T009.

### Tests that keep the foundation green

- [X] T011 [P] Edit `CORE_TEST/emailverification/internal/model/EmailVerificationTest.java`: pass a purpose to every `issue` and `reconstitute` call, run the existing cases (issue stamps times, rejects non-positive lifetime, verify once, expiry boundary) for both purposes with a `@ParameterizedTest` over `VerificationPurpose`, and add a case that a null purpose is rejected with "purpose is required". Depends on T004.
- [X] T012 Edit `CORE_TEST/emailverification/internal/application/EmailVerificationTestFakes.java`: make the class and the nested `Repository`, `Generator`, and `Notifier` `public` so tests in the `user` package can reuse them. Key `Repository.records` by purpose and token, implement the new three-method port (`invalidateForUser(purpose, userId)` removes only unverified records of that purpose and user), and add `Optional<EmailVerification> outstandingFor(VerificationPurpose purpose, long userId)` returning the user's unverified proof of that purpose. Depends on T005.
- [X] T013 [P] Create `CORE_TEST/emailverification/internal/application/EmailVerificationIssuerTest.java`: for each purpose, `issue` returns a proof whose `expiresAt - createdAt` equals `EmailVerificationIssuer.LIFETIME` (24 hours) and whose purpose matches; issuing twice replaces the first (first token no longer found, second found); issuing `REGISTRATION` leaves an `EMAIL_CHANGE` proof of the same user in place and the reverse; the order of repository calls is invalidate, save; a non-positive custom lifetime is rejected; nothing is sent (the issuer has no notifier). Depends on T006, T012.
- [X] T014 Edit `CORE_TEST/emailverification/internal/application/EmailVerificationIssuanceCoordinatorTest.java` (constructor calls at lines 20, 34 and 43): build the coordinator as `new EmailVerificationIssuanceCoordinator(new EmailVerificationIssuer(...), notifier)`, keep every existing expected outcome, and assert the stored proof has purpose `REGISTRATION` and the notifier received the same token and expiry. Depends on T007, T012.
- [X] T015 [P] Edit `CORE_TEST/emailverification/internal/application/RequestEmailVerificationServiceTest.java` (constructor call at line 30): same wiring change as T014; expected outcomes unchanged. Depends on T007, T012.
- [X] T016 [P] Edit `CORE_TEST/emailverification/internal/application/VerifyEmailServiceTest.java` (the `reconstitute` helper at line 84): pass `VerificationPurpose.REGISTRATION`; expected outcomes unchanged. Depends on T004, T012.
- [X] T017 Edit `CORE_TEST/user/internal/application/UserAccountApplicationServiceTest.java` (coordinator construction at lines 163 and 584 only): use the new `(issuer, notifier)` constructor. Leave the pending-email-change parts of this file for T024. Depends on T007, T012.
- [X] T018 [P] Edit `INFRA_TEST/support/AccountFixtures.java` (the `EmailVerification.issue` call at line 47): pass `VerificationPurpose.REGISTRATION`. Depends on T004.
- [X] T019 [P] Edit `INFRA_TEST/adapter/outbound/verification/write/redis/VerificationRecordMapperTest.java`: call `toModel` with a purpose and assert the model carries it; run the round-trip case for both purposes. Depends on T009.
- [X] T020 Edit `INFRA_TEST/adapter/outbound/verification/write/redis/RedisEmailVerificationRepositoryTest.java` (the proof helper at line 39): add a purpose parameter, update every repository call to pass it, and keep all existing cases passing for `REGISTRATION`. Depends on T010.
- [X] T021 **Checkpoint**: from `services/identity-access-next`, run `./mvnw -pl identityaccess-core,identityaccess-infrastructure -am verify`. Expected: green, with `PendingEmailChange` and its adapter still present, and the registration flow behaving exactly as before. Depends on T003 to T020.

**Checkpoint**: The shared mechanism exists and registration runs through it. User story work can begin.

---

## Phase 3: User Story 1 - One Mechanism for Every Flow (Priority: P1) 🎯 MVP

**Goal**: Email change issues, stores, finds, and invalidates its proof through the shared mechanism with purpose `EMAIL_CHANGE`, and the duplicate model, port, adapter, record, and mapper are deleted.

**Independent Test**: Register and verify an account, then request and confirm an email change; both work as before. `git grep -n PendingEmailChange -- services/identity-access-next` returns nothing under `src/`, and the lifetime of 24 hours is defined only in `EmailVerificationIssuer`.

### Tests for User Story 1 (rewire first, expect compile failures until the services change)

- [X] T022 [US1] Edit `CORE_TEST/user/internal/application/UserAccountTestFakes.java`: remove `InMemoryPendingEmailChangeRepository` (lines 77 to 105) and the imports of `PendingEmailChange` and `PendingEmailChangeRepository`. Keep `RecordingEmailChangeNotifier` and `SequentialTokenGenerator`. Depends on T012.
- [X] T023 [US1] Edit `CORE_TEST/user/internal/application/EmailChangeApplicationServiceTest.java`: build `RequestEmailChangeService(accounts, hasher, issuer, notifier)` and `ConfirmEmailChangeService(verifications, accounts, clock)` with `EmailVerificationTestFakes.Repository` and an `EmailVerificationIssuer` over the same repository, clock, and `SequentialTokenGenerator`; read the outstanding proof with `outstandingFor(EMAIL_CHANGE, 42)`; replace the lifetime assertion at line 268 (`EmailVerificationIssuanceCoordinator.DEFAULT_LIFETIME`) with `EmailVerificationIssuer.LIFETIME`. Every expected result (`Result(true/false)`, `Confirmed`, `AlreadyConfirmed`, `InvalidOrExpired`, the thrown exceptions) stays as written. Depends on T022, T006.
- [X] T024 [US1] Edit `CORE_TEST/user/internal/application/UserAccountApplicationServiceTest.java` (the `SoftDeleteUserAccountService` wiring and the pending-change assertions): use `EmailVerificationTestFakes.Repository`, seed an `EMAIL_CHANGE` proof for the account, and assert closing the account removes it. Expected outcomes unchanged. Depends on T017, T022.
- [X] T025 [US1] Port any case from `CORE_TEST/user/internal/model/PendingEmailChangeTest.java` that `CORE_TEST/emailverification/internal/model/EmailVerificationTest.java` (T011) does not already cover into the latter, using the renamed vocabulary (`email`, `verify`, `isVerified`, `verifiedAt`). Compare the two files case by case and list in the commit message anything dropped on purpose. Depends on T011.
- [X] T026 [US1] Port any case from `INFRA_TEST/adapter/outbound/emailchange/write/redis/RedisPendingEmailChangeRepositoryTest.java` and `.../PendingEmailChangeRecordMapperTest.java` that is not already covered into `RedisEmailVerificationRepositoryTest.java` and `VerificationRecordMapperTest.java`, parameterized over both purposes (save and find round trip, expiry set on both keys, user pointer replaced by a newer proof, confirmed proof not removed by invalidate, blank token yields empty, digest only in the key and never the raw token). Depends on T020, T019.
- [X] T027 [P] [US1] Add a compatibility case to `INFRA_TEST/adapter/outbound/verification/write/redis/RedisEmailVerificationRepositoryTest.java`: for each purpose, write a token hash and user pointer directly with the `StringRedisTemplate` in the layout the old code produced (`userId`, `email`, `createdAt`, `expiresAt` in epoch microseconds, and the matching key prefix) and assert the new repository finds it, redeems it with `verify` plus `save`, and invalidates it. This pins spec FR-015 and SC-005. Depends on T020.

### Implementation for User Story 1

- [X] T028 [US1] Edit `CORE_MAIN/user/internal/application/command/RequestEmailChangeService.java`: constructor becomes `(UserAccountRepository repository, PasswordHasher passwordHasher, EmailVerificationIssuer issuer, EmailChangeNotifier notifier)`; remove the `LIFETIME` constant, `PendingEmailChangeRepository`, `Clock`, and `UniqueStringIdGenerator` fields. Keep the order of checks exactly: load account, build `Email`, `authorizeEmailChange` (returns `Result(false)` when unchanged), `findByEmail` taken check throwing `DuplicateUserAccountException("User account already exists")`. Then `EmailVerification change = issuer.issue(EMAIL_CHANGE, account.id(), newEmail)` and `notifier.sendEmailChange(newEmail, change.rawToken(), change.expiresAt())`, returning `Result(true)`. Depends on T006, T023.
- [X] T029 [US1] Edit `CORE_MAIN/user/internal/application/command/ConfirmEmailChangeService.java`: replace `PendingEmailChangeRepository` with `EmailVerificationRepository`; look up with `findByPresentedToken(EMAIL_CHANGE, command.token())`; use `email()` where it used `newEmail()`, `isVerified()` for `isConfirmed()`, and `verify(now)` for `confirm(now)`; in the two discard places call `invalidateForUser(EMAIL_CHANGE, account.id())`. Keep the sequence: blank token, not found or expired, already verified returns `AlreadyConfirmed`, account missing or not accessible returns `InvalidOrExpired`, account email already equals the proof (record it, `AlreadyConfirmed`), address taken (invalidate, throw `DuplicateUserAccountException`), apply, save with duplicate handling, then record and return `Confirmed`. The record step stays after the account save. Depends on T023.
- [X] T030 [US1] Edit `CORE_MAIN/user/internal/application/command/SoftDeleteUserAccountService.java`: replace the `PendingEmailChangeRepository pendingEmailChanges` field and constructor parameter with `EmailVerificationRepository verifications`, and call `verifications.invalidateForUser(VerificationPurpose.EMAIL_CHANGE, account.id())` where it called `discardForUser(account.id())`. Keep it inside the `if (changed)` block before the session revocation. Depends on T024.
- [X] T031 [US1] Delete the duplicate in core: `CORE_MAIN/user/internal/model/PendingEmailChange.java`, `CORE_MAIN/user/internal/port/repository/PendingEmailChangeRepository.java`, and `CORE_TEST/user/internal/model/PendingEmailChangeTest.java` (only after T025). Keep `CORE_MAIN/user/internal/port/EmailChangeNotifier.java`. Depends on T028, T029, T030, T025.
- [X] T032 [US1] Delete the duplicate in infrastructure: the folder `INFRA_MAIN/emailchange/` (`RedisPendingEmailChangeRepository.java`, `PendingEmailChangeRecord.java`, `PendingEmailChangeRecordMapper.java`) and `INFRA_TEST/adapter/outbound/emailchange/` (`RedisPendingEmailChangeRepositoryTest.java`, `PendingEmailChangeRecordMapperTest.java`), only after T026. `MailEmailChangeNotifier` stays where it is, in `verification/mail`. Depends on T031, T026.
- [X] T033 [US1] Edit `INFRA_TEST/structure/StructureRules.java`: remove the `PORT_MAP` row for `user.internal.port.repository.PendingEmailChangeRepository` (`RedisPendingEmailChangeRepository`, `emailchange.write.redis`). Leave the `EmailVerificationRepository` row (`RedisEmailVerificationRepository`, `verification.write.redis`) and the `EmailChangeNotifier` row as they are. Depends on T032.
- [X] T034 [US1] **Checkpoint**: from `services/identity-access-next`, run `./mvnw verify` (all modules, including the presentation slices and the e2e suite). Expected: green with no edit to any presentation or `identityaccess-start` source. Then run `git grep -n PendingEmailChange -- services/identity-access-next` and confirm it matches nothing under `src/`. Depends on T028 to T033.

**Checkpoint**: User Story 1 is complete. Registration and email change share one mechanism, and the duplicate is gone.

---

## Phase 4: User Story 2 - A Token Can Only Complete Its Own Kind of Action (Priority: P1)

**Goal**: Prove that a proof of one purpose cannot be found, redeemed, or invalidated through the other.

**Independent Test**: Present a registration token to the email-change confirm and an email-change token to the registration verify; both answer invalid and change nothing.

### Tests for User Story 2

- [X] T035 [P] [US2] Add to `INFRA_TEST/adapter/outbound/verification/write/redis/RedisEmailVerificationRepositoryTest.java` the cross-purpose cases against real Redis: a proof saved as `REGISTRATION` is not found by `findByPresentedToken(EMAIL_CHANGE, token)` and the reverse; the same raw token saved under both purposes yields two independent proofs; `invalidateForUser(EMAIL_CHANGE, user)` leaves the user's `REGISTRATION` proof and its keys in place and the reverse; the token digest for each purpose is stored only under its own key family (assert with `redis.keys(...)` on both patterns). Depends on T034.
- [X] T036 [P] [US2] Add to `CORE_TEST/emailverification/internal/application/VerifyEmailServiceTest.java` a case that an `EMAIL_CHANGE` proof (seeded in the fake repository) presented as the registration token returns `InvalidOrExpired`, the account stays `PENDING_VERIFICATION`, and the proof is not marked verified. Depends on T034.
- [X] T037 [US2] Add to `CORE_TEST/user/internal/application/EmailChangeApplicationServiceTest.java` a case that a `REGISTRATION` proof seeded in the fake repository, presented to `ConfirmEmailChangeService`, returns `InvalidOrExpired`, the account email is unchanged, and the proof is not marked verified. Depends on T034.

**Checkpoint**: Purpose separation is pinned at the repository level and at both services.

---

## Phase 5: User Story 3 - Account Lifecycle Still Cleans Up After Itself (Priority: P2)

**Goal**: Prove that replacement, taken-address conflict, resend, and account close still remove the right proof and only that one.

**Independent Test**: Request two email changes and the first link fails while the second works; close the account and the pending link fails; resend a registration email and only the newest link works.

### Tests for User Story 3

- [X] T038 [US3] In `CORE_TEST/user/internal/application/EmailChangeApplicationServiceTest.java`, confirm these cases exist after the rewiring and add whichever is missing: a second request replaces the first (first token `InvalidOrExpired`, second `Confirmed`); an address taken before confirm throws `DuplicateUserAccountException` and the proof is invalidated; a repeated confirm returns `AlreadyConfirmed`. Not parallel with T037, same file. Depends on T037.
- [X] T039 [P] [US3] In `CORE_TEST/user/internal/application/UserAccountApplicationServiceTest.java`, assert that closing an account invalidates its `EMAIL_CHANGE` proof and leaves a `REGISTRATION` proof of the same user untouched (seed both in the shared fake), and that a failed close (wrong password) invalidates nothing. Depends on T034.
- [X] T040 [P] [US3] In `CORE_TEST/emailverification/internal/application/RequestEmailVerificationServiceTest.java`, assert that a resend replaces the earlier registration proof (old token not found, new token notified once) and leaves an `EMAIL_CHANGE` proof of the same user untouched. Depends on T034.

**Checkpoint**: All three user stories are covered.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T041 [P] Add a short "Superseded by spec 006" note to `specs/005-account-change-endpoints/research.md` (section 4 on where the pending change is stored, and the package-boundary statement in section 5), `specs/005-account-change-endpoints/data-model.md` (the `PendingEmailChange` entity), and `specs/005-account-change-endpoints/plan.md` (the Structure Decision paragraph and the source tree entries for `PendingEmailChange*`). One or two sentences each, linking to `specs/006-unify-email-ownership-proof/`; do not rewrite the HTTP behavior those documents describe. Update `specs/005-account-change-endpoints/contracts/core-and-infra-changes.md` the same way for the removed port and adapter rows.
- [X] T042 From `services/identity-access-next`, run `./mvnw spotless:apply` and then `./mvnw spotless:check`; review the diff for formatting-only changes.
- [X] T043 From `services/identity-access-next`, run `./mvnw verify` once more on the formatted tree and confirm all modules are green.
- [X] T044 Run the review checks from `specs/006-unify-email-ownership-proof/quickstart.md`: `git grep -n PendingEmailChange -- services/identity-access-next` shows nothing under `src/`; the 24 hour lifetime appears only in `EmailVerificationIssuer`; and `git diff --stat HEAD -- '*.lua' 'services/identity-access-next/identityaccess-infrastructure/src/main/resources/templates' 'services/identity-access-next/identityaccess-presentation/src/main' 'services/identity-access-next/identityaccess-start/src/main'` is empty; no Flyway migration was added. Also run `git status --short` and confirm `services/identity-access-next/email.txt` is still untracked and unstaged.
- [ ] T045 Run the manual walkthrough from `specs/006-unify-email-ownership-proof/quickstart.md` against a running service: registration confirm and repeat, email-change confirm and repeat, the two cross-purpose requests answering `400 invalid_or_expired_token`, and the Redis key patterns `identity-access:verification:*` and `identity-access:email-change:*`. For the pre-release link check, register on the previous build, deploy this one, and open the old link.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Setup. Blocks every user story. Order inside it: T003, then T004, then T005, then T006 and T007; T008, T009, T010 follow T005 and T004 as noted; tests T011 to T020 follow their production tasks; T021 closes the phase.
- **User Story 1 (Phase 3)**: Depends on Phase 2. It is the heart of the feature and the MVP, because it removes the duplicate.
- **User Story 2 (Phase 4)**: Depends on User Story 1 for its service tests (T036, T037) because they need the email-change service on the shared repository; the repository test T035 needs only the Phase 2 code but edits the same test file as T026 and T027, so it follows T034.
- **User Story 3 (Phase 5)**: Depends on User Story 1 for the same reason.
- **Polish (Phase 6)**: Depends on all stories. T041 can start any time after T034.

### Task-level notes

- **Same-file edits are sequential**: `UserAccountApplicationServiceTest.java` (T017, T024, T039), `EmailChangeApplicationServiceTest.java` (T023, T037, T038), `RedisEmailVerificationRepositoryTest.java` (T020, T026, T027, T035), `VerifyEmailServiceTest.java` (T016, T036), `RequestEmailVerificationServiceTest.java` (T015, T040), `EmailVerificationTest.java` (T011, T025). The `[P]` marks refer to tasks on different files at the same point in the plan.
- **Deletions come last**: T031 waits for T025 and T032 waits for T026, so no test case is lost.

### Parallel Opportunities

- Phase 1: T001 and T002 are independent.
- Phase 2: after T004 and T005, T008, T009, and T011 can run together; T013, T015, T016, T018, and T019 are on separate files.
- Phase 3: T027 can run while T022 to T026 are being done.
- Phase 4: T035 and T036 are on different files and can run together.
- Phase 5: T039 and T040 can run together.
- Phase 6: T041 can run while T042 and T043 are in progress.

---

## Parallel Example: Foundational Phase

```text
# After T004 (model) and T005 (port) are done, launch together:
Task: "T008 Edit VerifyEmailService.java to pass REGISTRATION"
Task: "T009 Edit VerificationRecordMapper.java to take the purpose"
Task: "T011 Edit EmailVerificationTest.java to run both purposes"
```

---

## Implementation Strategy

### MVP First (through User Story 1)

1. Complete Phase 1 (green baseline).
2. Complete Phase 2, ending at the T021 checkpoint: registration runs on the shared mechanism, old email-change code still in place and green.
3. Complete Phase 3: move email change over and delete the duplicate. **Stop and validate** with T034 (`./mvnw verify`, `git grep`).
4. At this point the feature's goal is met. Phases 4 and 5 add the tests that pin the separation and cleanup guarantees and should land in the same change.

### Incremental Delivery and Commits

The change is delivered as one refactor on the branch, after the account-change work (`4707bfb`). Suggested commit boundaries so each one builds and passes: (1) Phase 2 through T021; (2) Phase 3 through T034; (3) Phases 4 to 6. Do not merge to `master` as part of this work; that is a separate decision.

---

## Notes

- **Build command used**: `./mvnw verify` fails today in `identityaccess-start` before this work (its pom binds a `check` goal to `spring-boot-maven-plugin`, which has no such goal), so every checkpoint was run with `./mvnw test`, which includes the e2e suite. The pom was not changed.
- **T045** (manual walkthrough against a running service and a real mailbox) is left for the service owner.

- Behavior must not change. If a test's expected outcome has to change to pass, stop: that is a defect in the implementation or a scope question, not a test fix.
- The Redis key names, hash fields, and the two Lua scripts must stay byte-identical (spec FR-013 and FR-015).
- `services/identity-access-next/email.txt` holds the owner's mail credentials. Never stage, read aloud, or commit it.
- Commit after each checkpoint; keep Spotless clean.
