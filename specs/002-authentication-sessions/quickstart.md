# Quickstart: Validate Authentication and Sessions

This guide validates the planned core use cases from input through session state and output. It does not start HTTP, database, or token-signing adapters, which are outside this feature. Contract details are in [commands.md](contracts/commands.md), [ports.md](contracts/ports.md), and [data-model.md](data-model.md).

## Prerequisites

- Java 21 and Maven are available.
- Work from `services/identity-access-next`, the active service workspace, rather than assuming the repository root is the Maven project.
- The implementation provides core tests with in-memory user/session ports, deterministic ID and refresh-credential generators, a fixed `Clock`, and a recording access-token issuer. No external service is required.

## Run the checks

```bash
mvn -pl identityaccess-core -am test
mvn -pl identityaccess-core -am spotless:check
```

Expected result after implementation: both commands finish with `BUILD SUCCESS`. The test command exercises the module's existing account/email-verification tests as well as new authentication model and application tests. The formatting command checks Java sources without rewriting them.

## Scenarios the core tests must prove

| Scenario | Setup and action | Expected outcome |
|----------|------------------|------------------|
| Successful sign-in | An `ACTIVE` account with a matching password and device label signs in | Exactly one `AuthenticationSession`; one access and one refresh credential; session carries a UUIDv7 ID and bounded expiry |
| Denied sign-in | Absent account, wrong password, or each inaccessible status (`PENDING_VERIFICATION`, `LOCKED`, `SUSPENDED`, `DELETED`) | Generic authentication failure; zero sessions and credentials |
| Repeated renewal | Renew twice using the original refresh credential | Two access credentials issued; no replacement refresh credential or extra session; `lastUsedAt` advances |
| Expiry boundary | Advance fixed clock to exactly `expiresAt` and renew | No access credential; session is expired at the boundary |
| Current sign-out | Sign out once and again using the same refresh credential | First result `true`, second `false`; later renewal fails |
| Selected sign-out | Correct password selects one owned session, then repeats; try wrong password and another account's ID | Only the first owned request revokes; all rejected/repeated requests leave other sessions unchanged |
| Other-session sign-out | Correct password and a usable owned current session, with several other retained sessions | Count equals newly revoked others; current session remains usable; repeating returns zero |
| Session view | Query a user with 100 retained sessions while another user has sessions | All 100 owned records appear in deterministic order; no foreign record, raw token, or digest appears |
| Account change | Change password, suspend, or delete an account with multiple sessions | Every old refresh credential fails afterward; no-op/rejected changes leave prior session state intact |
| Dependency failure | Make session creation, revocation, or access-token issuance fail in a fake | Failure propagates; caller receives no false success or credential |

## Integration gates for a later adapter delivery

When infrastructure and presentation are implemented, verify digest-only credential storage, atomic conditional session changes, account-save/revocation coordination, secure cookie settings, access-token signing, and the 95%-within-2-seconds outcome from the spec. These checks need real adapters and are not represented as runnable commands for this core-only plan.

## Implementation record

- **Baseline (T001)**: Before the feature changes, `mvn -pl identityaccess-core -am test` failed to compile with "package org.springframework... does not exist". Cause: the Maven local repository in this WSL environment resolves to a Windows path (`D:/Documents/Extension/M2`), so the Spring jars were not on the classpath. This is an environment issue, not a code issue. Passing `-Dmaven.repo.local=/mnt/d/Documents/Extension/M2` (with `-o` for offline) makes the baseline pass.
- **Core tests (T010, T014, T021, T025, T036, T037)**: `mvn -o -Dmaven.repo.local=/mnt/d/Documents/Extension/M2 -pl identityaccess-core -am test` runs 141 tests with 0 failures and 0 errors.
- **Formatting (T037)**: `spotless:check` passes after `spotless:apply` on the core module.
- **Cross-store atomicity (T021)**: Account-change revocation is synchronous and in-process only. Core cannot prove that a password change, suspension, or deletion and its session revocation commit together across independent stores. A future adapter must provide that guarantee before this behavior is considered production-safe.
- **Lifetime wiring**: `AuthenticateService` takes a `Duration` session lifetime from its constructor. The composition root must provide that bean; no adapter or Spring wiring for it exists in this feature.
