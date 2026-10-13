# Research: Registration Email Verification Core

**Date**: 2026-10-10  
**Sources**: `specs/001-email-verification-core/spec.md`, the registration portions of `services/identity-access-next/missing-core-behaviors-part-2-emailverification.txt`, and relevant `identityaccess-core` source.

## Decision 1: Keep registration and email change separate

**Decision**: This feature issues and verifies tokens for initial account registration and allows replacement while the account is pending. It does not change an existing account's email or status in response to an email-change request.

**Rationale**: The user's scope decision supersedes the source brief's combined feature. The current account model already uses `PENDING_VERIFICATION` for initial registration, and a later email-change feature can define separate eligibility and token behavior.

**Alternatives considered**: Retain the combined registration/email-change plan. Rejected because it would contradict the active spec and user's explicit split.

## Decision 2: Reuse the existing registration and account contracts

**Decision**: Add issuance only after `RegisterUserService` saves a newly registered pending account, before returning `Registered(userId)`. Preserve `VerificationPending` and `AccountAlreadyExists` without issuance. Use `UserAccount.activateAfterVerification()` for the pending-to-active transition, and use USER's `Email`, `UserId`, and `UserAccountRepository`.

**Rationale**: The current registration service is already transactional and exposes these result branches. The account transition already exists; `Email` normalizes comparison.

**Alternatives considered**: A new registration command or duplicate account state machine. Both add interfaces and state without changing required behavior.

## Decision 3: Share internal issuance, preserve public privacy

**Decision**: A shared internal issuance coordinator invalidates outstanding verification, creates one token, saves it, and synchronously requests delivery. Registration invokes it directly; the public replacement request calls it only after finding a pending account. Every valid email receives identical fieldless `Accepted`, and the core adds no limiter.

**Rationale**: A shared coordinator keeps the replacement rules the same for both entry points. The public request result must not reveal whether an account exists or is eligible.

**Alternatives considered**: Calling the public request use case from registration, adding a core throttling port, or using events/messages. These blur trusted and untrusted flows or exceed scope.

## Decision 4: Retain enough token state for result precedence

**Decision**: Blank, absent, invalidated, and expired tokens return `InvalidOrExpired`; unexpired consumed tokens return `AlreadyVerified`. An unused current token activates only a pending account whose normalized email equals the verification email. Replacement invalidates outstanding records but preserves consumed records through expiry.

**Rationale**: The results distinguish replay from replacement and prevent a stale token from activating a changed or ineligible account.

**Alternatives considered**: Deleting used records immediately, treating replaced tokens as replay, or allowing a stale account/email match. Each violates the spec's result matrix.

## Decision 5: Make lifetime deterministic

**Decision**: Inject `Clock` and a positive configurable `Duration`, defaulting to 24 hours. Capture time once at issuance, calculate expiry from it, and expire at `now >= expiresAt`.

**Rationale**: The expiry boundary is testable without sleeping, and deployment can change the lifetime without changing domain rules.

**Alternatives considered**: Hard-coded lifetime or direct system clock calls in verification logic. Both make the required boundary behavior less reliable to test.

## Decision 6: Define transactional behavior without claiming adapter proof

**Decision**: Keep registration and new command handlers transactional, with account and verification writes coordinated in the same transaction. Propagate generator, repository, and notifier failures. Keep the repository, generator, and notifier as plain Java ports. Later adapters must prove rollback, unique presented-token lookup, and concurrent single use.

**Rationale**: Current core services use `@Transactional` and core tests use fakes. Those tests can verify call order and propagated failures, but cannot prove real storage rollback or concurrency. A provider-accepted delivery may precede a failed commit; a still-pending account can request replacement.

**Alternatives considered**: Swallowing delivery failure, claiming `@Transactional` alone guarantees concurrency, or choosing a database/queue now. These either violate the spec or decide infrastructure outside scope.

## Decision 7: Validate within the service workspace

**Decision**: Run focused core tests, `mvn -pl identityaccess-core clean test`, `mvn spotless:check`, and `mvn test` from `services/identity-access-next` after implementation. Cover new registration, replacement, expiry boundary, replay, account mismatch, and failures.

**Rationale**: The workspace uses Java 21, Spring Boot 4.0.8, `spring-tx`/`spring-context`, and JUnit starter tests. The full reactor check catches integration effects without expanding implementation scope.

**Alternatives considered**: Treating core fakes as proof of adapter rollback or concurrent single use. Those claims require later integration tests.
