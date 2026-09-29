# Email Verification Core Behaviors Plan

## Goal

Implement every approved behavior in
`missing-core-behaviors-part-2-emailverification.txt`: the lifecycle model and ports,
non-enumerating resend, opaque-token verification, pending-account activation, and
synchronous registration/email-change delivery effects.

## Current-state audit

| Task | Status | Remaining gap |
| --- | --- | --- |
| Email-verification capability | Not implemented | Core has no `emailverification` package, model, ports, commands, services, or tests. |
| Activation boundary | Public command removed; coordinator pending | `UserAccount.activateAfterVerification()` permits only `PENDING_VERIFICATION -> ACTIVE`; the arbitrary-user-ID use case and service are removed, and Part 2 must provide the sole trusted token coordinator. |
| Registration integration | Ready | `RegisterUserService` saves a new pending account and returns `Registered(long userId)`, but creates no verification and sends nothing. |
| Email-change integration | Separate design required | `ChangeEmailService` currently changes the normalized email while the account remains `ACTIVE`. Part 2 must not reuse initial `PENDING_VERIFICATION`; a future flow must retain the verified email and model an unverified candidate separately. |
| Time support | Available | `identityaccess-start` already provides a UTC `Clock`. |
| Adapters | Not implemented | Verification persistence, secure token generation, rate limiting, and notification are absent. |

The last recorded baseline in `docs/savepoints/2026-09-27_13-26-part1-validation.md`
is a passing core clean test, Spotless check, and full reactor test. Re-establish it
before implementation because the working tree contains broad uncommitted changes.

## Assumptions and constraints

- Use `emailverification/command` only for public use cases and keep implementation
  under `internal/application`, `internal/model`, and `internal/port`.
- Name the model `EmailVerification`; do not add a generic token package, events,
  messaging, base aggregates, or a public activation command keyed by user ID.
- Reuse USER's `UserId`, `Email`, `UserAccount`, and `UserAccountRepository` rather
  than duplicating identity types.
- Keep tokens opaque. Infrastructure hashes tokens at rest and owns storage,
  serialization, URL construction, templates, and SMTP.
- Inject `Clock` and a configured verification lifetime. Do not hard-code wall-clock
  calls or the lifetime in the model.
- Keep coordination synchronous and propagate persistence/notifier failures rather
  than reporting success after partial work.
- Preserve one non-enumerating `Accepted` result for every syntactically valid request,
  including absent, ineligible, and throttled accounts.
- Only a newly created `Registered` account gets the registration delivery effect;
  existing-account and uniqueness-race outcomes are not implicit resends.
- Preserve unrelated dirty-worktree changes.

## Step-by-step plan

- [ ] 1. Settle policies and add the public contracts.
  - Add `RequestEmailVerificationUseCase.Command(String email) -> Accepted`; keep
    `Accepted` fieldless so it cannot reveal existence, eligibility, throttling, or
    provider status.
  - Add `VerifyEmailUseCase.Command(String token)` with sealed results
    `Verified(long userId)`, `InvalidOrExpired`, and `AlreadyVerified`.
  - Configure the verification lifetime and resend rate-limit policy. Decide the
    throttle window/key and whether a technology-neutral core limiter port or an outer
    application decorator owns enforcement before implementing request handling.
  - Treat blank tokens as `InvalidOrExpired`; normalize/validate email with `Email`.
  - Verify: contract tests compile and results expose no account or delivery details.

- [ ] 2. Add the lifecycle model and technology-neutral ports.
  - Model the token/verification reference, user ID, email, creation, expiry, and
    nullable verification time, with validated creation/reconstitution paths.
  - Add explicit expiration-at-boundary, single-use, consumed-state, and
    `verifiedAt` behavior. Keep account eligibility in the application coordinator.
  - Add `EmailVerificationRepository.save`, `findByPresentedToken`, and
    `invalidateForUser`; `VerificationTokenGenerator.generate`; and
    `VerificationNotifier.sendVerification` with exactly the task-file inputs.
  - If step 1 assigns throttling to core, add only the smallest technology-neutral
    limiter contract needed by the chosen policy.
  - Define the adapter contract: save hashes raw token material, presented-token lookup
    hashes its input, invalidation disables earlier outstanding tokens, and consumed
    records remain distinguishable for `AlreadyVerified`.
  - Test construction invariants, exact-boundary expiry, first/repeated consumption,
    and reconstitution.
  - Verify: focused tests pass and core ports contain no Redis, JDBC, JWT, SMTP, URL,
    template, serialization, or framework types.

- [ ] 3. Implement the public non-enumerating request/resend flow.
  - Normalize the email and apply the approved rate limit without exposing the
    decision in `Accepted`.
  - Look up the account by email. For a missing or non-pending account, return
    `Accepted` without token generation, persistence, invalidation, or notification.
  - For an eligible pending account, generate one raw token; calculate creation and
    expiry from the injected clock/lifetime; invalidate earlier verifications; save
    the replacement; and notify with the same normalized email, token, and expiry.
  - Keep invalidation and replacement persistence in one transaction. Propagate
    generator, repository, and notifier failures. Document that provider acceptance is
    an external side effect and cannot be rolled back after sending.
  - Test missing, active, locked, suspended, deleted, pending, and throttled cases;
    replacement ordering; argument consistency; normalization; and failures.
  - Verify: all valid-email branches have the same result shape, and only an eligible,
    unthrottled pending account reaches delivery.

- [ ] 4. Make token verification the sole activation coordinator.
  - Resolve through `findByPresentedToken`; map missing, blank, and expired tokens to
    `InvalidOrExpired` without mutations.
  - Return `AlreadyVerified` for a consumed verification without repeating activation
    or persistence.
  - For an unconsumed, unexpired record, require an existing
    `PENDING_VERIFICATION` account whose current email matches the verification email.
    Treat a missing account, changed email, or any other state as `InvalidOrExpired`.
  - In one transaction, record `verifiedAt`, activate the account, save both models,
    and return `Verified(userId)` only after both saves succeed.
  - Keep the removed `user.api.command.ActivateAfterVerificationUseCase` and
    `ActivateAfterVerificationService` absent; only the trusted verification
    coordinator may invoke the domain transition. Update USER tests accordingly.
  - Test blank/missing, expired, consumed, wrong-email, missing-account, non-pending,
    successful, repeated-presentation, timestamp, and either-save-failure cases.
  - Verify: repository-wide search finds no public arbitrary-ID activation path, and
    only a valid, current, unexpired, single-use token can activate an account.

- [ ] 5. Integrate synchronous creation/delivery with USER commands.
  - Share one internal creation/delivery coordinator between public resend,
    registration, and email change; do not recursively call the non-enumerating public
    endpoint from trusted internal flows.
  - After `RegisterUserService` saves a new account, create and request its initial
    verification in the same command/transaction before returning `Registered`.
  - Do not send for `AccountAlreadyExists`, `VerificationPending`, or the uniqueness
    race reread path.
  - Do not route email changes through initial-account verification or move the account
    to `PENDING_VERIFICATION`. Introduce a separate unverified-email candidate flow
    that retains the verified email while the account remains `ACTIVE`; do nothing for
    unchanged or rejected changes.
  - Keep USER dependent only on a narrow core coordinator, never on presentation or
    infrastructure classes.
  - Test exactly one request for new registration and changed email; no requests on
    every no-op/rejected/existing path; old-address replacement; and failure/rollback
    behavior.
  - Verify: mark the deferred Part 1 email-verification checklist complete only after
    its transaction and failure tests pass.

- [ ] 6. Validate and record adapter obligations.
  - Run focused model/application tests, then a clean core test, Spotless check, and
    full reactor test.
  - Search for a generic token package, messaging/events, public arbitrary-ID
    activation, adapter technology in core ports, and raw-token logging/exposure.
  - When adapters are implemented, integration-test token hashing, presented-token
    lookup, resend invalidation, consumed-token classification, expiry, and atomic
    verification-consumption/account-activation rollback.
  - Contract-test notifier URL/template/provider behavior in infrastructure without
    moving those details into core.
  - Update this plan and the Part 1 deferred checklist from observed results, then
    record a savepoint before handing off to Part 3.

## Files likely to change

### Core production

- New public contracts under
  `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/emailverification/command/`
- New model, services, coordinator, and ports under
  `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/emailverification/internal/`
- `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/user/internal/application/command/RegisterUserService.java`
- `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/user/internal/application/command/ChangeEmailService.java`
- Already removed by the USER status-policy work; keep absent:
  - `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/user/api/command/ActivateAfterVerificationUseCase.java`
  - `identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess/user/internal/application/command/ActivateAfterVerificationService.java`

### Tests and configuration

- New model/application tests and fakes under
  `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/emailverification/`
- `identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/user/internal/application/UserAccountApplicationServiceTest.java`
- A focused verification-policy configuration under `identityaccess-start` if the
  lifetime is bound there; reuse its existing UTC `Clock` bean.
- `docs/plans/user-core-behaviors-plan.md` after deferred Part 2 checks are proven.
- A new validation savepoint under `docs/savepoints/`.

### Deferred adapters and endpoints

- Persistence, secure token generation, throttling, notification, and migrations under
  `identityaccess-infrastructure`.
- Request/verify endpoints and result mapping under `identityaccess-presentation` only
  when a later task assigns presentation work.

## Verification plan

1. Compile public contracts and run model tests before adding coordination.
2. Test request/resend with deterministic clock, generator, limiter, repository, and
   notifier fakes.
3. Test verify behavior for every invalid/consumed branch and both save failures.
4. Test registration and email-change coordination, including all no-effect paths.
5. Run `mvn -pl identityaccess-core clean test`.
6. Run `mvn spotless:check` and `mvn test` from the active workspace reactor root.
7. Run targeted `rg` checks for forbidden architecture, technology leakage,
   arbitrary-ID activation, and token exposure.
8. When adapters exist, run database transaction, hashing, invalidation, expiry, and
   notifier contract integration tests before claiming end-to-end completion.

## Risks and open questions

- Rate limiting is required but its window, key, accounting, and ownership are not
  specified, and no limiter appears in the listed ports. Resolve this before step 3.
  Prefer a technology-neutral boundary with an infrastructure-backed implementation.
- The verification lifetime is unspecified. Bind it as configuration and freeze it in
  tests rather than choosing a model/service magic value.
- `save(EmailVerification)` must convey enough opaque token material for the adapter to
  hash it, while reconstituted models must never turn stored hashes into deliverable
  tokens. Confirm the exact token-reference representation before implementation.
- Synchronous mail acceptance is irreversible: notifier failure can abort a database
  transaction, but mail acceptance followed by commit failure may deliver a dead link.
  An outbox would address that edge case but is explicitly outside this no-messaging
  task, so document the accepted failure contract.
- PostgreSQL can atomically consume verification and activate the account. Redis expiry
  cannot participate in the normal PostgreSQL account transaction and needs an
  explicitly accepted consistency strategy before use.
- `AlreadyVerified` requires consumed records to remain resolvable. Deleting them
  immediately collapses the result to `InvalidOrExpired` and violates the contract.
- USER currently uses `user.api.command`, while the Part 2 target explicitly says
  `emailverification.command`; follow the Part 2 target without expanding scope into a
  USER package migration.
- The worktree is broadly dirty; implementation must not revert or reformat unrelated
  service or repository changes.
