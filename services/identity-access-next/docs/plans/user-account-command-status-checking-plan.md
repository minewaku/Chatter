# User Account Command Status Checking Plan
## Goal
Define and enforce how every `AccountStatus` affects USER commands. Existing-account
commands must load the current `UserAccount`, let the aggregate accept, reject, or
idempotently ignore the operation, and save only an actual change. Query handlers stay
free of account-lifecycle prechecks.
## Current-state findings
- The commands already read the database. Every existing-account service uses
  `UserAccountCommandService.getAccount(long)`, which calls
  `UserAccountRepository.findById(...)`. Registration uses `findByEmail(...)`.
- `UserAccount` already guards lifecycle changes with `transition(...)`, guards
  password/username/email changes with `verifyAccessibleCredentials(...)`, and makes
  deletion terminal.
- The model exposes `status()` and `isAccessible()`, but it has no named predicates
  for pending, active, locked, suspended, or deleted states.
- Tests cover the main transitions and a happy-path command sequence, but not the full
  action-by-status matrix or repository read/save behavior for rejected and no-op
  commands.
- `ActivateAfterVerificationUseCase` accepts only a user ID. The existing Part 2 plan
  correctly requires activation to move behind verified-token coordination.
- Baseline observed during research: `mvn -pl identityaccess-core test` passes all 15
  tests.
## Assumptions and constraints
- `PENDING_VERIFICATION` means only that a newly created account has not completed its initial verification. It is not reused for later email-change verification.
- `ACTIVE` is the normal enabled state and the only accessible status.
- Status rules are domain invariants and belong in `UserAccount`, not in controllers
  or duplicated `switch` statements in command services.
- A command loads the aggregate once. Do not add a separate status-only repository
  query, because its result could diverge from the aggregate being mutated.
- A valid transition returns `changed=true` and is saved once. Being in the requested
  target status is an idempotent `changed=false` result and is not saved. Every other
  source status throws `InvalidAccountStateTransitionException` and is not saved.
- For credential-protected commands, reject an inaccessible status before matching the
  password.
- `DELETED` is terminal and cannot be restored. A repeated deletion is a no-op.
- Queries may return immutable projections for any status without these lifecycle
  checks. Authentication, authorization, and privacy filtering remain separate query
  boundary concerns.
- Preserve unrelated files and changes. This plan does not implement production code.
## Command/status policy
Legend: **Change** means mutate and save, **No-op** means return
`changed=false` without saving, and **Reject** means throw
`InvalidAccountStateTransitionException` without saving.
### Credential-protected commands
| Current status | Change password | Change username | Change email |
| --- | --- | --- | --- |
| `PENDING_VERIFICATION` | Reject | Reject | Reject |
| `ACTIVE` | Change if the validated value differs; otherwise no-op | Change if the normalized value differs; otherwise no-op | Change if the normalized value differs and remain `ACTIVE`; otherwise no-op |
| `LOCKED` | Reject | Reject | Reject |
| `SUSPENDED` | Reject | Reject | Reject |
| `DELETED` | Reject | Reject | Reject |
For `ACTIVE`, an incorrect current password throws `InvalidCredentialsException`
before mutation or persistence.
Changing an email must not reuse `PENDING_VERIFICATION`, because that status represents
initial account verification. If a later email-change verification flow is added, keep
the account `ACTIVE`, retain the currently verified email, and model the unverified new
email separately until confirmation succeeds.
### Lifecycle commands
| Current status | Activate after verification | Lock | Unlock | Suspend | Reinstate | Soft delete |
| --- | --- | --- | --- | --- | --- | --- |
| `PENDING_VERIFICATION` | Change to `ACTIVE` | Reject | Reject | Reject | Reject | Change to `DELETED` |
| `ACTIVE` | No-op | Change to `LOCKED` | No-op | Change to `SUSPENDED` | No-op | Change to `DELETED` |
| `LOCKED` | Reject | No-op | Change to `ACTIVE` | Reject | Reject | Change to `DELETED` |
| `SUSPENDED` | Reject | Reject | Reject | No-op | Change to `ACTIVE` | Change to `DELETED` |
| `DELETED` | Reject | Reject | Reject | Reject | Reject | No-op |
Activation here is an internal aggregate transition. Only successful validation of a
current, unexpired, single-use email-verification token may coordinate it; an arbitrary
public user-ID command must not remain.
### Registration
Registration has no existing user ID, so it normalizes the email and calls
`findByEmail(...)`.
| Account found by normalized email | Result | Write behavior |
| --- | --- | --- |
| None | `Registered(userId)` | Create one `PENDING_VERIFICATION` account and save once |
| `PENDING_VERIFICATION` | `VerificationPending` | No mutation or save |
| `ACTIVE` | Existing-account result | No mutation or save |
| `LOCKED` | Existing-account result | No mutation or save |
| `SUSPENDED` | Existing-account result | No mutation or save |
| `DELETED` | Existing-account result if an exact match somehow remains | No mutation or save |
A properly deleted account has an anonymized email, so registration using its former
email normally creates a new pending account rather than restoring the deleted one.
The current name `AccountAlreadyActive` is inaccurate for locked and suspended
accounts. Prefer `AccountAlreadyExists` unless API compatibility requires retaining
the old name. Do not reveal the exact non-pending status. Apply the same classification
after the uniqueness-race reread.
## Aggregate API strategy
Keep `AccountStatus` semantics explicit and documented:
- `PENDING_VERIFICATION`: newly created account awaiting initial verification.
- `ACTIVE`: verified, enabled, and the only accessible state.
- `LOCKED`: temporarily blocked for security reasons.
- `SUSPENDED`: administratively suspended.
- `DELETED`: terminal soft-deleted state.
`isAccessible()` remains a derived property of the status. Add named status predicates
to `UserAccount` only when they improve domain behavior or tests; do not expand the
public aggregate API solely for readability.
Mutation methods remain the authoritative guards. Retain
`transition(expected, target, operation)` and introduce or rename a private
`requireAccessible(operation)` helper as needed. Do not expose a generic
`canPerform(String)`, and do not make a service call a predicate before calling the
mutation; either approach duplicates policy and permits callers to bypass invariants.
## Step-by-step plan
- [x] 1. Freeze the policy as parameterized domain tests.
  - Exercise every lifecycle command against all five statuses.
  - Assert target states, target-state idempotency, rejection of every other source,
    and unchanged `updatedAt` for no-op/rejected operations.
  - Exercise password, username, and email changes against all statuses, including
    incorrect credentials and equal normalized values for `ACTIVE`.
  - Prove that changing an email on an `ACTIVE` account does not move it to
    `PENDING_VERIFICATION`.
  - Preserve deletion anonymization and prove all later mutations are rejected.
  - Verify: `mvn -pl identityaccess-core -Dtest=UserAccountTest test`.
- [x] 2. Document account-status semantics and consolidate private guards.
  - Add concise Javadoc to every `AccountStatus` value so initial verification, active access, security lock, administrative suspension, and terminal deletion cannot be confused.
  - Keep `isAccessible()` derived from the status definition; add named aggregate predicates only where they are actually needed.
  - Preserve status-first credential validation.
  - Keep decisions inside `UserAccount`; do not move the matrix into
    `AccountStatus` or application services.
  - Verify: all domain matrix tests pass without service changes.
- [x] 3. Prove command-service loading and persistence behavior.
  - Add `findById` and `findByEmail` counters to `InMemoryRepository`.
  - For each existing-account command, prove one aggregate lookup occurs.
  - Prove a change saves once, a no-op saves zero times, a rejected status saves zero
    times, and a missing account fails before mutation.
  - Keep the load -> aggregate operation -> `saveIfChanged` service shape. Renaming
    `getAccount` to `requireAccount` is optional.
  - Verify:
    `mvn -pl identityaccess-core -Dtest=UserAccountApplicationServiceTest test`.
- [x] 4. Resolve registration vocabulary and cover its complete status matrix.
  - Decide whether `AccountAlreadyActive` can become `AccountAlreadyExists`.
  - Add pending, active, locked, suspended, and reconstituted-deleted cases.
  - Cover the uniqueness-conflict path without assuming that a failed insert can always
    be followed by a reread in the same transaction. Let the persistence strategy define
    whether the conflict is translated directly, retried after rollback, or reread in a
    separate transaction.
  - Prove no existing-account branch creates or saves a second account.
  - Verify: registration lookup/result/save assertions all pass.
- [x] 5. Secure activation when email verification is implemented.
  - Remove or internalize `ActivateAfterVerificationUseCase` and its service.
  - In the verification coordinator, require a pending account whose email matches the
    verification, then consume the verification and activate the account atomically when
    both operations share one transactional resource. Otherwise, make the coordination
    idempotent and resilient to partial failure.
  - Reject missing, expired, consumed, wrong-email, and non-pending paths without
    account mutation.
  - Verify: no public activation command accepts only a user ID.
  - Implementation note: email verification is not present yet. This plan removes the
    unsafe arbitrary-ID activation API now; token validation, consumption, and atomic
    coordination remain assigned to `email-verification-core-behaviors-plan.md` and
    must be completed before activation is exposed through any coordinator.
- [x] 6. Make status-agnostic query behavior explicit and perform final validation.
  - Keep queries on `UserAccountViewRepository`; do not load `UserAccount` merely to
    precheck status.
  - Add a parameterized query test returning a projection for every status.
  - Run focused tests, a clean core build, formatting checks, and the full reactor.
  - Search for duplicated command-status branching outside `UserAccount`, allowing
    registration classification, verification coordination, persistence mapping, and
    projection mapping.
## Files likely to change
- `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/user/internal/model/AccountStatus.java`
- `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/user/internal/model/UserAccount.java`
- `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/user/internal/application/command/UserAccountCommandService.java` only if its lookup method is renamed
- `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/user/internal/application/command/RegisterUserService.java` if the result is renamed
- `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/user/api/command/RegisterUserUseCase.java` if the result is renamed
- `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/user/internal/model/UserAccountTest.java`
- `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/user/internal/application/UserAccountApplicationServiceTest.java`
- `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/user/internal/application/UserAccountTestFakes.java`
- Part 2 email-verification coordinator files when activation is secured
`AccountStatus.java` documents state meaning and derives accessibility, but it does not
need to own the command/action matrix. No query production class needs a lifecycle status
check.
## Verification plan
1. Run the focused domain and application-service tests.
2. Run `mvn -pl identityaccess-core clean test`.
3. Run `mvn spotless:check`.
4. Run `mvn test` from the active service workspace.
5. Use `rg` to detect duplicated status branching and the unsafe activation API.
6. When a concrete repository is available, integration-test optimistic locking so a
   concurrently changed status cannot be overwritten from stale aggregate state.
## Risks and open questions
- Resolved: `AccountAlreadyActive` was renamed to `AccountAlreadyExists`; the only
  in-repository production consumer was updated and the clean reactor compiles.
- This plan treats `LOCKED` and `SUSPENDED` as mutually exclusive account states. Confirm
  that the business does not need an account to be both security-locked and administratively
  suspended at the same time; otherwise the status model must be split before implementation.
- This plan preserves target-state idempotency, including activate, unlock, and
  reinstate while already `ACTIVE`. Confirm if those should instead be invalid.
- Query status agnosticism is not an authorization or deletion-privacy bypass; those
  remain separate read-boundary policies.
- Aggregate checks protect in-memory invariants, but concurrency safety still depends
