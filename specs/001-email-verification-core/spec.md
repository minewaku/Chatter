# Feature Specification: Registration Email Verification Core

**Feature Branch**: `master`
**Created**: 2026-10-09
**Status**: Draft
**Input**: User direction on 2026-10-10 and `services/identity-access-next/missing-core-behaviors-part-2-emailverification.txt`, limited to registration verification.

## Clarifications

### Session 2026-10-10

- Q: Which email-verification flow is in scope for this feature? → A: Verify the email of a newly registered account only; email changes for existing accounts belong to a separate feature.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Verify a New Account (Priority: P1)

A person registers with an email address, receives a verification token for that address, and uses it to activate the new account.

**Why this priority**: Registration must prove control of the supplied email before the new account becomes active.

**Independent Test**: Register a new account, present the delivered token before expiry, and confirm that the matching pending account becomes active exactly once.

**Acceptance Scenarios**:

1. **Given** an email not associated with an existing account, **When** registration succeeds, **Then** one pending account and one verification token for its email are created, one delivery is requested before registration returns, and the existing `Registered(userId)` result is preserved.
2. **Given** an email associated with an existing pending account, **When** registration is attempted again, **Then** the existing `VerificationPending` result is returned and no new token or delivery is requested.
3. **Given** an email associated with another existing account, **When** registration is attempted, **Then** the existing `AccountAlreadyExists` result is returned and no new token or delivery is requested.
4. **Given** an unused, unexpired token for a pending account whose current email matches the token email, **When** the token is presented, **Then** the token is consumed, the account becomes active, and `Verified(userId)` is returned only after both changes succeed.
5. **Given** a consumed token that has not expired, **When** it is presented again, **Then** `AlreadyVerified` is returned and the account is not changed again.

---

### User Story 2 - Replace a Lost Registration Token (Priority: P2)

A person with a newly registered account that is still pending verification can request another token for the same email.

**Why this priority**: A lost or expired message must not permanently block registration.

**Independent Test**: Request a replacement for a pending account, verify that the earlier outstanding token no longer works, and use the replacement to activate the account.

**Acceptance Scenarios**:

1. **Given** a pending account, **When** verification is requested for its current email, **Then** any previous outstanding token is invalidated, one replacement token is created, one delivery is requested, and a fieldless `Accepted` result is returned.
2. **Given** an email with no account, or an account that is not pending verification, **When** verification is requested, **Then** the same fieldless `Accepted` result is returned with no token creation or delivery.
3. **Given** a token invalidated by replacement, **When** it is presented, **Then** `InvalidOrExpired` is returned, while the replacement remains usable until it expires or is replaced.
4. **Given** a malformed email, **When** verification is requested, **Then** the existing email validation rejects it.

### Edge Cases

- Blank, missing, invalidated, and expired tokens return `InvalidOrExpired`. A token expires at the exact moment current time reaches its expiry.
- A consumed token returns `AlreadyVerified` only before expiry; an expired consumed token returns `InvalidOrExpired`.
- An unused token returns `InvalidOrExpired` if its account is missing, is no longer pending verification, or no longer has the email recorded with the token.
- Pending accounts cannot perform protected account changes; replacement requests and token verification remain available.
- Failures while saving account or token changes, or requesting delivery, propagate to the caller and are never reported as success.
- If a delivery provider accepts a request but a later commit fails, the delivered token may be unusable; the pending account can recover by requesting a replacement.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: This feature MUST cover registration email verification only: token issuance for a newly registered account, replacement while that account remains pending, and token-based activation. Email changes for existing accounts, endpoints, delivery adapters, storage technology, rate limiting policy, and message templates are outside this feature.
- **FR-002**: Successful new-account registration MUST create a pending account, create one verification token for its current email, request one delivery before returning `Registered(userId)`, and preserve the existing registration results. Existing-account results and rejected registration MUST create no token and request no delivery.
- **FR-003**: `RequestEmailVerificationUseCase.Command(String email)` MUST validate the email using existing rules and return the same fieldless `Accepted` result for every valid email, whether the account is absent, ineligible, or pending. Only a pending account MAY receive a replacement token and delivery request. The outer layer owns throttling.
- **FR-004**: A replacement MUST invalidate the account's previous outstanding token before creating one new token and requesting its delivery. An invalidated token MUST return `InvalidOrExpired`; a previously consumed token MUST remain distinguishable until expiry.
- **FR-005**: `VerifyEmailUseCase.Command(String token)` MUST return only `Verified(userId)`, `InvalidOrExpired`, or `AlreadyVerified`. A blank, missing, invalidated, or expired token MUST return `InvalidOrExpired`; an unexpired consumed token MUST return `AlreadyVerified`.
- **FR-006**: An unused, unexpired token MUST activate only its matching pending account. If the account is missing, has another status, or has a different current email, verification MUST return `InvalidOrExpired` without consuming the token or changing the account.
- **FR-007**: Successful verification MUST consume the token and change the account from `PENDING_VERIFICATION` to `ACTIVE` as one atomic account-and-token change. `Verified(userId)` MUST be returned only when both changes succeed. The same token MUST NOT activate the account more than once.
- **FR-008**: Each token MUST identify one account and one email, be single-use, and record creation, expiry, and optional consumption time. Its default validity period MUST be 24 hours and MUST be configurable; creation and expiry decisions MUST use a consistent supplied time source. Expiry begins when current time reaches the token's expiry.
- **FR-009**: A raw token MAY be retained for matching by the verification store, but MUST NOT appear in public results or logs. The generated token MUST be unpredictable to a recipient who has not received it.
- **FR-010**: Failures while saving account or token changes, or while requesting delivery, MUST propagate rather than produce a successful result.
- **FR-011**: No public action that activates an account using only its account ID MUST be exposed. Pending accounts MUST remain barred from protected account changes until verification succeeds.

### Key Entities *(include if feature involves data)*

- **User account**: An existing user-domain account with an ID, current email, and status. Registration creates it in `PENDING_VERIFICATION`; successful verification moves it to `ACTIVE`.
- **Email verification**: A replaceable, expiring record associated with one account and the email being proved. It contains a token, account ID, email, creation time, expiry time, and optional consumed time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Each successful new registration requests exactly one delivery before returning; existing-account and rejected registrations request zero.
- **SC-002**: Every valid-email replacement request returns the same fieldless result, and only a pending account causes one new delivery.
- **SC-003**: After replacement, the earlier outstanding token cannot activate the account; the latest unexpired token can.
- **SC-004**: A matching unexpired token activates one pending account exactly once; replay before expiry returns `AlreadyVerified` without changing the account.
- **SC-005**: Every blank, missing, invalidated, expired, account-mismatched, or status-mismatched token produces `InvalidOrExpired` without activating an account.
- **SC-006**: No storage or delivery failure is reported as successful registration, replacement, or verification.

## Assumptions

- The user's 2026-10-10 decision narrows this feature to registration verification. The source brief's email-change behavior belongs to a separate feature and does not govern this spec.
- In the current account model, `PENDING_VERIFICATION` means initial registration is awaiting email verification. A later email-change feature will define its own eligibility and token behavior.
- Existing account identity and email validation rules determine email equality and registration outcomes.
- Delivery request means the notification boundary accepted the request; final arrival in an inbox is outside this feature.
- The source brief's core-only package, port, and transaction expectations remain inputs to implementation planning where they concern registration verification.
