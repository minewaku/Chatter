---
description: "Dependency-ordered tasks for registration email verification core"
---

# Tasks: Registration Email Verification Core

**Input**: `specs/001-email-verification-core/{spec,plan,research,data-model,quickstart}.md`, `specs/001-email-verification-core/contracts/`, and the registration portions of `services/identity-access-next/missing-core-behaviors-part-2-emailverification.txt`.

**Tests**: Required by the source brief and the plan's validation contract. Add each story's tests before its implementation and observe the expected failure.

**Organization**: Tasks follow the two user stories in the active spec. Code paths below are relative to the active workspace, `services/identity-access-next`; `specs/` paths are relative to the Git root. `[P]` means tasks touch distinct files and can run concurrently after their stated prerequisites.

## Phase 1: Setup

**Purpose**: Establish the existing core baseline without changing project structure or sibling services.

- [X] T001 Run `mvn -pl identityaccess-core clean test` from `services/identity-access-next/pom.xml` and record pre-existing failures before changing `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/user/internal/application/command/RegisterUserService.java`.

**Checkpoint**: Core baseline is known.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build the verification lifecycle and technology-neutral ports shared by registration and replacement.

- [X] T002 [P] Add failing lifecycle tests in `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/emailverification/internal/model/EmailVerificationTest.java` for required token/account/email/creation/expiry fields, optional consumed time, positive lifetime, single use, reconstitution, and expiry at `now >= expiresAt`.
- [X] T003 [P] Define `VerificationTokenGenerator.generate() -> String` in `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/emailverification/internal/port/VerificationTokenGenerator.java`; keep the port free of provider or storage types.
- [X] T004 [P] Define `VerificationNotifier.sendVerification(Email, String rawToken, Instant expiresAt) -> void` in `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/emailverification/internal/port/VerificationNotifier.java`; include no URL, template, or provider type.
- [X] T005 Implement `EmailVerification` in `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/emailverification/internal/model/EmailVerification.java` with `rawToken` as an opaque presented secret absent from public results/logs; `userId` referencing one registered account; `email` equal to the account's current normalized email at activation; `createdAt` from the supplied `Clock`; `expiresAt = createdAt + configured Duration` (default 24 hours); and optional `verifiedAt`, absent while unused and set on successful consumption. Require positive duration, support reconstitution of used and unused records, make `now >= expiresAt` expired, and make T002 pass.
- [X] T006 Define `EmailVerificationRepository.save(EmailVerification)`, `findByPresentedToken(String) -> Optional<EmailVerification>`, and `invalidateForUser(UserId)` in `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/emailverification/internal/port/EmailVerificationRepository.java`; invalidated outstanding records must no longer resolve, while consumed records remain distinguishable until expiry.
- [X] T007 Add deterministic repository, generator, and notifier fakes in `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/emailverification/internal/application/EmailVerificationTestFakes.java`; model replacement invalidation, consumed-token retention, delivery arguments/order, and injectable failures without claiming database rollback.

**Checkpoint**: The model and ports compile, model tests pass, and test fakes are ready. No adapter or limiter has been added.

---

## Phase 3: User Story 1 - Verify a New Account (Priority: P1) 🎯 MVP

**Goal**: Registration requests one verification delivery; presenting the matching token activates the new pending account exactly once.

**Independent Test**: Register a new account using core fakes, capture its token, verify before expiry, then replay; expect one delivery, `Verified(userId)`, active status, then `AlreadyVerified`.

### Tests for User Story 1

- [X] T008 [P] [US1] Add failing verification tests in `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/emailverification/internal/application/VerifyEmailServiceTest.java` for blank/missing/invalidated tokens, exact expiry boundary, unexpired replay, missing account, non-`PENDING_VERIFICATION` status, normalized-email mismatch, successful consume-plus-activate, and propagated account/token save failures.
- [X] T009 [P] [US1] Add failing issuance tests in `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/emailverification/internal/application/EmailVerificationIssuanceCoordinatorTest.java` for one token using a fixed `Clock`, `createdAt + Duration` expiry (default 24 hours, configurable positive duration), prior outstanding invalidation, identical saved/notified token and expiry, call order, and generator/repository/notifier failures.
- [X] T010 [P] [US1] Extend `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/user/internal/application/UserAccountApplicationServiceTest.java` with failing new-registration tests for exactly one synchronous delivery before `Registered`, zero issuance on `VerificationPending`/`AccountAlreadyExists` or rejected registration, and propagated issuance failure; update its direct `RegisterUserService` construction for the new collaborator.

### Implementation for User Story 1

- [X] T011 [P] [US1] Define `VerifyEmailUseCase.Command(String token)` and sealed `Verified(long userId) | InvalidOrExpired | AlreadyVerified` results in `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/emailverification/command/VerifyEmailUseCase.java`; expose neither raw tokens in results nor activation by account ID.
- [X] T012 [P] [US1] Implement shared synchronous issuance in `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/emailverification/internal/application/EmailVerificationIssuanceCoordinator.java`: inject `Clock` and a positive configurable `Duration` defaulting to 24 hours, invalidate outstanding tokens, save one verification, request delivery with the same token/email/expiry, propagate failures, and make T009 pass.
- [X] T013 [P] [US1] Implement `@Transactional` token verification in `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/emailverification/internal/application/VerifyEmailService.java`: apply invalid/expiry/used precedence, load by `UserId`, require pending status and equal current `Email`, consume and activate in one transaction, save both, return `Verified(userId)` only on success, and make T008 pass.
- [X] T014 [P] [US1] Update `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/user/internal/application/command/RegisterUserService.java` to invoke internal issuance only after saving a newly registered pending account and before returning `Registered`; preserve existing-account results and the current transaction, and make T010 pass.
- [X] T015 [US1] Run the US1 tests and `mvn -pl identityaccess-core test` from `services/identity-access-next/pom.xml`; confirm the registration-to-verification journey, replay, mismatch, and failure results documented in `specs/001-email-verification-core/quickstart.md`.

**Checkpoint**: New-account registration verification works independently as the MVP. No replacement request is needed to prove this story.

---

## Phase 4: User Story 2 - Replace a Lost Registration Token (Priority: P2)

**Goal**: A still-pending account can replace its token while every valid-email request returns the same fieldless result.

**Independent Test**: Request twice for a pending account and verify only the newest token works; absent, active, locked, suspended, and deleted accounts return identical `Accepted` without issuance.

### Tests for User Story 2

- [X] T016 [US2] Add failing request tests in `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/emailverification/internal/application/RequestEmailVerificationServiceTest.java` for existing `Email` normalization/validation, fieldless `Accepted` across pending/absent/ineligible addresses, zero generation/delivery for ineligible accounts, old-token `InvalidOrExpired` after replacement, consumed-token retention, and propagated repository/notifier failures.

### Implementation for User Story 2

- [X] T017 [US2] Define `RequestEmailVerificationUseCase.Command(String email) -> Accepted` with fieldless `Accepted` in `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/emailverification/command/RequestEmailVerificationUseCase.java`.
- [X] T018 [US2] Implement `@Transactional` request handling in `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/emailverification/internal/application/RequestEmailVerificationService.java`: use existing `Email` validation and normalized lookup, call shared issuance only for `PENDING_VERIFICATION`, return identical `Accepted` for every valid email, add no core limiter, and make T016 pass.
- [X] T019 [US2] Run the US2 tests and `mvn -pl identityaccess-core test` from `services/identity-access-next/pom.xml`; confirm replacement invalidation and non-enumerating results against `specs/001-email-verification-core/quickstart.md`.

**Checkpoint**: Both registration verification and pending-account replacement pass independently within the core-only scope.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Validate the complete registration-only behavior and architecture boundaries.

- [X] T020 Run `mvn -pl identityaccess-core clean test` from `services/identity-access-next/pom.xml` and confirm every core scenario in `specs/001-email-verification-core/quickstart.md`, including expiry, replay, replacement, account mismatch, registration effects, and failure paths.
- [X] T021 Run `mvn spotless:check` and `mvn test` from `services/identity-access-next/pom.xml`; resolve only feature-caused failures in `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/emailverification/` and affected registration-core files.
- [X] T022 Audit `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/emailverification/command/` and `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/emailverification/internal/port/` for token leakage, infrastructure types, events/messaging, generic token packages, core rate limiting, public activation by account ID, and accidental email-change behavior; retain adapter rollback/concurrency tests as later obligations in `specs/001-email-verification-core/quickstart.md`.

**Checkpoint**: Registration-only core checks pass and deferred adapter guarantees remain explicit.

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup (Phase 1) establishes the baseline.
- Foundation (Phase 2) follows Setup and blocks both stories. T005 follows T002; T006 follows T005; T007 follows T003-T006.
- US1 (Phase 3) follows Foundation. T008-T010 are written before implementation; T013 needs T011 and T014 needs T012. T015 follows all US1 work.
- US2 (Phase 4) follows US1 because its independent journey uses US1's issuer and verification handler. T016 precedes T017-T018; T019 follows implementation.
- Polish (Phase 5) follows both stories.

### User Story Dependency Graph

```text
Setup → Foundation → US1 → US2 → Polish
```

### Parallel Opportunities

- T002, T003, and T004 touch different files after T001.
- T008, T009, and T010 are separate test files after Foundation.
- T011 and T012 can proceed together after their red tests; T013 and T014 can proceed together after their own prerequisites, touching separate files.
- US2 is sequential after US1 because replacement reuses the issuance and verification components.

### Parallel Example: User Story 1

```text
After T007: T008 VerifyEmailServiceTest.java | T009 EmailVerificationIssuanceCoordinatorTest.java | T010 UserAccountApplicationServiceTest.java
After red tests: T011 VerifyEmailUseCase.java | T012 EmailVerificationIssuanceCoordinator.java
After T011/T012: T013 VerifyEmailService.java | T014 RegisterUserService.java
```

### Parallel Example: User Story 2

```text
After US1: T016 RequestEmailVerificationServiceTest.java
After T016 fails: T017 RequestEmailVerificationUseCase.java → T018 RequestEmailVerificationService.java → T019 core validation
```

## Implementation Strategy

### MVP First

Complete T001-T015. This yields new-account issuance, token-gated activation, and deterministic core tests without an endpoint or mail adapter.

### Incremental Delivery

1. Complete Foundation and US1; validate registration-to-verification and replay.
2. Add US2; validate uniform request results and replacement invalidation.
3. Complete Polish; later adapters must separately prove real rollback and concurrent single use.

## Notes

- A provider-accepted delivery cannot be rolled back after a later commit failure; a still-pending account can request replacement.
- `@Transactional` coordinates core writes, but fake-based tests cannot prove actual database rollback or concurrent single use.
- The user's registration-only scope supersedes the combined source brief's email-change tasks. Do not modify `ChangeEmailService` or `UserAccount.changeEmail` for this feature.
