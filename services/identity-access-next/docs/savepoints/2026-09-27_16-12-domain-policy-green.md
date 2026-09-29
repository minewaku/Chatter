# Goal

Implement `docs/plans/user-account-command-status-checking-plan.md` within `identity-access-next` while keeping status policy in the aggregate and persistence conditional on actual changes.

# Current step

Domain policy checkpoint complete. Plan steps 1 and 2 are checked; next is command-service load/save behavior.

# Files changed

- `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/user/internal/model/UserAccountTest.java`: full parameterized lifecycle and credential-command matrix with timestamp, idempotency, status-first rejection, normalized no-op, and deletion assertions.
- `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/user/internal/model/UserAccount.java`: active email changes no longer reuse `PENDING_VERIFICATION`.
- `docs/plans/user-account-command-status-checking-plan.md`: steps 1 and 2 marked complete.
- Preserved the pre-existing `AccountStatus.java` Javadocs that document each state and derive accessibility from the enum value.

# Validation status

- `mvn -pl identityaccess-core -Dtest=UserAccountTest test`: pass, 51 tests, 0 failures/errors/skips.

# Remaining steps

1. Add repository lookup counters and prove load/save/rejection/missing-account behavior for every existing-account command.
2. Rename the registration result and cover all statuses plus the uniqueness-race path.
3. Resolve the contingent activation-coordinator task without expanding into the separate Part 2 implementation.
4. Add parameterized status-agnostic query coverage and run all final validations/searches.

# Open issues

- Email verification is not implemented, so plan step 5 remains contingent on the separate Part 2 plan.
- The source plan ends with an incomplete final risk sentence; no missing requirement has been inferred.
