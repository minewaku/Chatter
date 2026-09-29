# Goal

Implement the user-account command/status plan with precise aggregate policy, persistence behavior, and non-enumerating registration classification.

# Current step

Registration checkpoint complete. Plan steps 1-4 are checked; next is the contingent activation task and status-agnostic query/final validation step.

# Files changed

- `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/user/api/command/RegisterUserUseCase.java`: renamed `AccountAlreadyActive` to `AccountAlreadyExists`.
- `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/user/internal/application/command/RegisterUserService.java`: returns the status-neutral existing-account result.
- `identityaccess-presentation/src/main/java/com/minewaku/chatter/identityaccess/presentation/user/AuthController.java`: consumes the renamed result without changing HTTP behavior.
- `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/user/internal/application/UserAccountTestFakes.java`: counts generated IDs.
- `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/user/internal/application/UserAccountApplicationServiceTest.java`: covers normalized registration, all five existing statuses, and all five uniqueness-race reread classifications with lookup/ID/save assertions.
- `docs/plans/email-verification-core-behaviors-plan.md`: updated the dependent result-name reference.
- `docs/plans/user-account-command-status-checking-plan.md`: step 4 marked complete.

# Validation status

- `mvn -pl identityaccess-core -Dtest=UserAccountApplicationServiceTest clean test`: pass, 50 tests, 0 failures/errors/skips.
- `mvn -pl identityaccess-presentation -am -DskipTests compile`: pass.

# Remaining steps

1. Resolve plan step 5, which is contingent on unimplemented email verification and unresolved Part 2 policy inputs.
2. Add a parameterized query projection test for every status.
3. Run focused tests, clean core tests, Spotless, full reactor tests, and targeted architecture searches.
4. Synchronize every plan checkbox and write the final savepoint.

# Open issues

- No concrete `UserAccountRepository` adapter exists. The fake exercises the current reread strategy; the eventual persistence adapter must integration-test conflict handling outside an aborted database transaction.
- Email-verification coordination cannot be implemented safely until the separate Part 2 plan's lifetime, rate-limit, token-reference, and consistency choices are approved.
