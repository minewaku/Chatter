# Goal

Implement the user-account command/status plan without leaving an arbitrary-user-ID activation boundary.

# Current step

Activation-boundary checkpoint complete. Plan steps 1-5 are checked; only status-agnostic query coverage and final validation remain.

# Files changed

- Removed `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/user/api/command/ActivateAfterVerificationUseCase.java`.
- Removed `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/user/internal/application/command/ActivateAfterVerificationService.java`.
- `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/user/internal/application/UserAccountApplicationServiceTest.java`: removed public activation service cases and retained aggregate transition coverage in `UserAccountTest`.
- `docs/plans/user-account-command-status-checking-plan.md`: step 5 marked complete with an explicit deferral note for the absent verification coordinator.

# Validation status

- `mvn -pl identityaccess-core -Dtest=UserAccountApplicationServiceTest clean test`: pass, 46 tests, 0 failures/errors/skips.
- Targeted `rg` search: no public arbitrary-ID activation use case/service remains in production modules.

# Remaining steps

1. Parameterize query projection coverage over all five statuses.
2. Run focused tests, clean core build, Spotless, full reactor tests, and duplicate-status-branch searches.
3. Mark plan step 6 complete, ensure every checklist item is checked, and write the final savepoint.

# Open issues

- The Part 2 email-verification plan remains unimplemented and has unresolved lifetime, rate-limit, token-reference, and consistency choices. Its coordinator must be the only future caller that activates after validated verification.
- No concrete repository exists yet for optimistic-lock or uniqueness-conflict integration tests.
