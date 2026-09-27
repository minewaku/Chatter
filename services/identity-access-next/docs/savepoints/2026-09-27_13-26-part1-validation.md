# Savepoint: Part 1 validation

## Goal

Complete the actionable Part 1 USER behaviors from `docs/plans/user-core-behaviors-plan.md` without widening scope.

## Current step

Plan items 1?3 are complete and verified. Items 4?5 remain deferred by their own declared dependencies: there is no Part 2 email-verification capability or Part 3 refresh-session capability in this repository.

## Files changed

- `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/user/internal/model/UserAccountTest.java` ? complete USER transition, deletion, and credential/identity mutation coverage.
- `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/user/internal/application/UserAccountTestFakes.java` ? deterministic in-memory test adapters.
- `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/user/internal/application/UserAccountApplicationServiceTest.java` ? command/query, persistence/no-op, typed registration, and uniqueness-race coverage.
- `identityaccess-presentation/src/main/java/com/minewaku/chatter/identityaccess/presentation/user/ApiExceptionHandler.java` ? explicit imports required by Spotless.
- `identityaccess-presentation/src/main/java/com/minewaku/chatter/identityaccess/presentation/user/AuthController.java` ? explicit imports required by Spotless.
- `identityaccess-presentation/src/main/java/com/minewaku/chatter/identityaccess/presentation/user/RegisterRequest.java` ? explicit imports required by Spotless.
- `identityaccess-presentation/src/main/java/com/minewaku/chatter/identityaccess/presentation/user/UserController.java` ? explicit imports required by Spotless.
- `docs/plans/user-core-behaviors-plan.md` ? items 1?3 checked after verification.

## Validation status

- `mvn -pl identityaccess-core clean test` passed: 15 tests, 0 failures/errors/skips.
- `mvn spotless:apply` and `mvn spotless:check` passed across the reactor.
- Active-module stale-reference search passed with no results.
- V1 schema check passed: exactly the five approved statuses and no password-modification timestamp.
- `mvn test` passed across all five reactor modules; no later module was skipped because of a core failure.

## Remaining steps

1. Implement Part 2 email verification, then execute plan item 4 with atomicity/failure tests.
2. Implement Part 3 refresh sessions, then execute plan item 5 with revocation and transaction-boundary tests.

## Open issues

- The Part 2 and Part 3 dependencies are absent. Creating no-op or speculative ports/services would violate the plan constraints, so their checkboxes are intentionally still open.
