# User Core Behaviors Completion Plan

## Goal

Finish only the incomplete work from
`missing-core-behaviors-part-1-user-tasks.txt`, preserving the USER behavior already
implemented in the current working tree and deferring cross-capability effects until
the Part 2 and Part 3 capabilities exist.

## Current-state audit

| Task | Status | Evidence and remaining gap |
| --- | --- | --- |
| 1. Status semantics and suspend/reinstate rename | Implemented; verification incomplete | `AccountStatus`, `UserAccount`, command contracts, services, and V1 schema use `SUSPENDED` and enforce the requested transition sources. The old domain test still calls removed `disable()`/`enable()` methods, so the transition matrix is not currently verified. Activation is structurally limited to `PENDING_VERIFICATION`, but proof of a valid verification must be supplied by Part 2. |
| 2. `PasswordHasher.matches` | Implemented in core | The core port has both `hash` and `matches`. No concrete hasher adapter currently exists in this repository; core tests need a deterministic fake. |
| 3. Change password | Implemented; untested | The command, result, service, accessible-account check, current-password match, different-password check, hashing, and conditional save exist. No five-minute interval or `passwordModifiedAt` is retained. |
| 4. Registration outcomes | Implemented; untested | `Registered(long userId)`, `AccountAlreadyActive`, and `VerificationPending` exist, including a uniqueness-race reread. Application tests do not yet prove outcome classification or save behavior. |
| 5. CQS repository split | Implemented in core | Commands use `UserAccountRepository`; queries use `UserAccountViewRepository` and immutable `UserAccountView`. Concrete read/write persistence adapters are not present and remain infrastructure work. |
| 6. Cross-capability effects | Blocked by declared dependencies | There is no `emailverification` or `authentication` implementation. Registration/email-change verification requests and refresh-session revocation cannot yet be wired honestly. |
| 7. Change username | Implemented; untested | The command, result, service, credential/access checks, unchanged-value result, mutation, and conditional save exist. |
| 8. Change email | Partially implemented | The command, result, service, credential/access checks, normalized unchanged-value handling, transition to `PENDING_VERIFICATION`, and `verificationRequired` result exist. Requesting a new verification remains gated on Part 2. |
| 9. Terminal anonymizing deletion | Implemented; verification incomplete | `softDelete()` anonymizes email, username, and password hash, sets `DELETED`, and has no restoration path. Tests do not yet cover deletion from every non-deleted status or rejection of later operations. |

Observed baseline on 2026-09-27:

- `mvn test` fails in `UserAccountTest` because it still references removed
  `disable()` and `enable()` methods; later reactor modules are skipped.
- `mvn spotless:check` fails with 32 core files requiring formatting/line-ending
  normalization.

## Assumptions and constraints

- Preserve all unrelated and pre-existing dirty-worktree changes.
- Treat `ACTIVE` as the only accessible state.
- Keep repeated operations idempotent only when the account is already in the requested
  target state; do not create additional state transitions.
- Do not add `passwordModifiedAt` or a five-minute password-change interval.
- Do not add messaging, no-op collaborators, speculative verification/session ports, or
  infrastructure technology types to core.
- Do not add account restoration. Deletion remains terminal and anonymizing.
- Keep state-changing contracts in `user.api.command`, lookup contracts in `user.api.query`, and
  models/ports under `user.internal`.

## Remaining implementation plan

- [x] 1. Repair and complete domain-level USER tests.
  - Replace stale `disable()`/`enable()` calls with `suspend()`/`reinstate()`.
  - Cover every allowed transition:
    `PENDING_VERIFICATION -> ACTIVE`, `ACTIVE -> LOCKED -> ACTIVE`,
    `ACTIVE -> SUSPENDED -> ACTIVE`, and every non-deleted state to `DELETED`.
  - Cover rejected cross-state operations, especially unlock/reinstate from
    `PENDING_VERIFICATION`, and idempotent calls in an operation's target state.
  - Cover terminal deletion, anonymized identity/credentials, and rejection of all
    post-deletion state and identity/credential changes.
  - Cover password, username, and email changes for success, incorrect current
    password, inaccessible account, unchanged normalized value, and changed email
    returning to `PENDING_VERIFICATION`.
  - Verify before continuing: `mvn -pl identityaccess-core test` passes the model tests.

- [x] 2. Add application-service tests for the already-present behavior.
  - Add reusable in-memory/fake implementations for `UserAccountRepository`,
    `UserAccountViewRepository`, `PasswordHasher`, and `UserIdGenerator` in test scope.
  - Verify change-password hashes and saves exactly once only when changed.
  - Verify change-username and change-email save only on change, and that email results
    report `verificationRequired` only when a new email is accepted.
  - Verify registration returns each typed outcome, does not create duplicate accounts,
    and correctly classifies a uniqueness race after rereading by email.
  - Verify state-command services persist only actual changes and query services read
    only immutable projections through `UserAccountViewRepository`.
  - Fix production code only where these tests expose a mismatch with the approved task
    file; do not expand the public API.
  - Verify before continuing: all core tests pass from a clean compilation.

- [x] 3. Normalize and validate the currently actionable Part 1 work.
  - Run the configured Spotless apply/check cycle and review the result so formatting
    does not obscure behavioral diffs.
  - Search active source for stale `DISABLED`, `DisableUserAccountUseCase`,
    `EnableUserAccountUseCase`, `disable()`, and `enable()` references.
  - Run the complete Maven reactor test suite and confirm later modules are no longer
    skipped because of core failures.
  - Verify the V1 status constraint contains exactly the approved statuses and has no
    unused password-modification timestamp.

- [ ] 4. Integrate email verification when Part 2 is implemented.
  - Make a newly `Registered` account synchronously request email verification.
  - Make a successful changed-email result synchronously request a replacement
    verification for the new normalized email.
  - Make `VerifyEmailUseCase` the trusted coordinator for activation and remove or
    narrow the public arbitrary-user-ID activation contract so activation cannot bypass
    valid, unexpired, single-use verification.
  - Add transaction/failure tests proving account state and verification creation do
    not report success independently.

- [ ] 5. Integrate refresh-session revocation when Part 3 is implemented.
  - Revoke all refresh sessions after a successful password change, suspension, or
    first soft deletion.
  - Do not revoke on rejected commands or unchanged/idempotent results.
  - Add coordination tests for success, no-op, failure, and transaction boundaries.

## Files likely to change

### Current Part 1 completion

- `identityaccess-core/src/test/java/.../user/internal/model/UserAccountTest.java`
- New tests and test fakes under
  `identityaccess-core/src/test/java/.../user/internal/application/`
- Existing USER production classes only if the new tests expose a task mismatch
- Java sources touched by Spotless normalization

### Deferred dependency integration

- Part 2 email-verification application services and USER registration/email-change
  coordination
- Part 3 authentication session port/service and USER password/suspension/deletion
  coordination
- `ActivateAfterVerificationUseCase` and
  `ActivateAfterVerificationService` when activation ownership moves to Part 2

## Verification plan

1. Run focused domain tests after transition and mutation coverage is repaired.
2. Run application tests with fakes after each command/query behavior is covered.
3. Force a clean core compilation to prevent stale `target` output from hiding source
   errors.
4. Run `mvn spotless:check`.
5. Run `mvn test` from the reactor root.
6. Use targeted `rg` searches for stale status/operation names and forbidden adapter
   technology in core ports.
7. When Parts 2 and 3 land, run their coordination tests plus the full reactor suite
   before marking the gated checklist items complete.

## Risks and open questions

- The current implementation is uncommitted in a broad dirty worktree; completion work
  must avoid reverting or rewriting unrelated changes.
- Registration outcome classification for existing LOCKED and SUSPENDED accounts is not explicitly defined. Decide whether these verified non-deleted accounts map to AccountAlreadyActive or whether the result contract should use a state-neutral name. Do not let the persistence adapter invent this policy.
- The repository currently has no concrete USER repository, password-hasher, or ID
  generator adapters. That does not invalidate the core port work, but the application
  cannot be proven runnable end-to-end until infrastructure adapters are supplied.
- `ActivateAfterVerificationUseCase.Command(long userId)` can currently be invoked by
  application code without verification evidence. There is no HTTP endpoint for it,
  but Part 2 must close this boundary rather than treating the name alone as proof.
- A uniqueness conflict caused only by an existing username cannot map to one of the
  email-oriented registration outcomes after rereading by email. Confirm the desired
  public result before persistence adapter work if username collision must be reported
  rather than rejected as a validation/conflict error.
