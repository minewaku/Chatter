# Goal

Implement every actionable item in `docs/plans/user-account-command-status-checking-plan.md` within `identity-access-next`.

# Current step

Complete. All six plan checklist items are checked and final validation is green.

# Files changed

- `AccountStatus.java`: preserved and formatted the pre-existing per-status Javadocs and accessibility definition.
- `UserAccount.java`: active email changes remain `ACTIVE`; aggregate-owned lifecycle and credential guards remain authoritative.
- `RegisterUserUseCase.java`, `RegisterUserService.java`, and `AuthController.java`: renamed the existing-account result to `AccountAlreadyExists`; uniqueness conflicts now escape the failed transaction for persistence-level retry after rollback.
- Removed `ActivateAfterVerificationUseCase.java` and `ActivateAfterVerificationService.java`, eliminating arbitrary-user-ID activation.
- `UserAccountTest.java`: parameterized the complete lifecycle/credential status matrix and timestamp/idempotency/deletion behavior.
- `UserAccountApplicationServiceTest.java` and `UserAccountTestFakes.java`: proved one-load/save-only-on-change behavior, complete registration classification and retry behavior, and status-agnostic queries for every status.
- Synchronized the source plan and dependent email-verification plan.

# Validation status

- Focused domain and application suites: pass, 101 tests total.
- `mvn -pl identityaccess-core clean test`: pass, 101 tests.
- `mvn spotless:check`: pass for all five reactor modules.
- `mvn clean test`: pass for the full five-module reactor; core runs 101 tests with zero failures/errors/skips.
- Architecture searches: only registration classification branches on account status outside `UserAccount`; query services use only `UserAccountViewRepository`; no stale `AccountAlreadyActive` production reference or public activation API remains.
- `git diff --check`: no whitespace errors; Git emits only a line-ending advisory for the dependent Markdown plan.

# Remaining steps

- None for this plan.
- Implement the separately approved email-verification plan before introducing any activation coordinator or email-change candidate verification flow.
- Add optimistic-lock, uniqueness-conflict, and transaction integration tests when a concrete account repository adapter exists.

# Open issues

- The business assumptions that `LOCKED` and `SUSPENDED` are mutually exclusive and target-state lifecycle commands are idempotent remain as approved by this plan.
- Email-verification lifetime, throttling, token storage, notification, and cross-resource consistency remain deliberately outside this implementation.
