# Goal

Implement `docs/plans/user-account-command-status-checking-plan.md` with aggregate-owned status policy and one-load/save-only-on-change command handling.

# Current step

Command-service persistence checkpoint complete. Plan steps 1-3 are checked; next is registration vocabulary and full status coverage.

# Files changed

- `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/user/internal/application/UserAccountTestFakes.java`: added `findById` and `findByEmail` counters.
- `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/user/internal/application/UserAccountApplicationServiceTest.java`: added parameterized changed/no-op/rejected/missing-account cases for all nine existing-account services.
- `docs/plans/user-account-command-status-checking-plan.md`: step 3 marked complete.

# Validation status

- `mvn -pl identityaccess-core -Dtest=UserAccountApplicationServiceTest test`: pass, 41 tests, 0 failures/errors/skips.
- Every existing-account service performs one `findById`; only changed operations save.

# Remaining steps

1. Rename `AccountAlreadyActive` to `AccountAlreadyExists` and cover pending/active/locked/suspended/deleted registration plus uniqueness conflicts.
2. Resolve the contingent activation-coordinator step without implementing the separate unresolved Part 2 plan.
3. Add parameterized status-agnostic query coverage and run focused, clean core, Spotless, full reactor, and architecture searches.

# Open issues

- Email verification does not yet exist, so activation coordination remains contingent.
- The source plan's final risk sentence is incomplete.
