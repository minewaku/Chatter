# Goal

Implement `docs/plans/user-account-command-status-checking-plan.md` within `identity-access-next` while keeping account-status policy in `UserAccount`, loading existing accounts once, saving only changes, and preserving status-agnostic queries.

# Current step

Checkpoint 1: the full domain command/status matrix is frozen in parameterized tests. The focused test run intentionally exposes the existing email-change status mismatch before production correction.

# Files changed

- `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/user/internal/model/UserAccountTest.java`: added parameterized lifecycle coverage for all five statuses, inaccessible credential-command coverage, status-first rejection checks, no-op/rejection timestamp assertions, and the requirement that an active email change remains active.

# Validation status

- `mvn -pl identityaccess-core -Dtest=UserAccountTest test`: expected red result, 51 tests run with exactly one failure. The failing assertion shows `changeEmail` returned `PENDING_VERIFICATION` instead of preserving `ACTIVE`; all other matrix cases passed.

# Remaining steps

1. Correct `UserAccount.changeEmail`, rerun the domain suite, synchronize plan steps 1-2.
2. Add command-service load/save counters and the full service behavior matrix.
3. Rename the registration result and test all status/race branches.
4. Resolve the contingent activation-coordinator task without expanding into the separate Part 2 implementation.
5. Add status-agnostic parameterized query tests and run final validation/searches.

# Open issues

- `AccountStatus.java` already contains uncommitted Javadocs from before this implementation; preserve them.
- Email verification is not implemented in this workspace, so the plan's step 5 is contingent on the separate Part 2 plan.
- The source plan ends with an incomplete final risk sentence; do not invent missing requirements.
