# Quickstart: Validate Registration Email Verification Core

Use this guide after implementation. Run commands from `services/identity-access-next` with Java 21 and Maven available. The [data model](data-model.md), [command contracts](contracts/commands.md), and [port contracts](contracts/ports.md) define expected states, results, and test fakes.

## 1. Run core behavior tests

```bash
mvn -pl identityaccess-core clean test
```

Expected: core tests pass using a fixed clock, deterministic token generator, in-memory account/verification repositories, and recording notifier. The suite should prove:

1. New registration requests exactly one delivery; existing-account results request none.
2. A pending account's replacement request returns fieldless `Accepted`, invalidates the outstanding token, and makes the old token `InvalidOrExpired`. Missing and ineligible accounts return the same result without delivery.
3. A current unexpired token for the matching pending account returns `Verified(userId)` and activates once. Replay before expiry returns `AlreadyVerified`; replay at or after expiry returns `InvalidOrExpired`.
4. Blank, missing, invalidated, expired, wrong-status, missing-account, and email-mismatch tokens return `InvalidOrExpired` without activation.
5. Repository, generator, and notifier failures propagate; coordination tests check delivery and save order without claiming real database rollback.

## 2. Run workspace checks

```bash
mvn spotless:check
mvn test
```

Expected: formatting and the Maven reactor tests pass. This feature does not require an endpoint, persistence adapter, limiter, mail template, or email-change implementation.

## 3. Review boundaries

Confirm the only new public verification commands are in `emailverification/command`, internals are in `emailverification/internal/{application,model,port}`, and no public activation by account ID exists. Confirm tokens are absent from results and logs. Check against the active [spec](spec.md), whose registration-only scope overrides the combined source brief.

## Later adapter validation

Core fake tests cannot prove database rollback or concurrent single use. When adapters are implemented, run integration tests showing that activation and consumption roll back together, concurrent presentation activates only once, invalidated tokens no longer resolve, and consumed tokens remain distinguishable until expiry. A provider-accepted delivery followed by a failed commit can leave a dead link; a still-pending account can request replacement.
