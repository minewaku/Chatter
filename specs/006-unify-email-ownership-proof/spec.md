# Feature Specification: Unify Email Ownership Proof

**Feature Branch**: `identity-access-next`

**Created**: 2026-10-19

**Status**: Draft

**Input**: User description: "Read this conversation as input" — the preceding review of `PendingEmailChange` and `EmailVerification`: the email-change flow keeps its own copy of the "prove you own this mailbox" mechanism (a pending change that is field-for-field the same as a registration verification), although the email verification area is the one that exists to prove mailbox ownership. Conclusion reached: the proof of mailbox ownership should live in one place that serves both registration and email change, with no change in what people or clients see.

## Clarifications

### Session 2026-10-19

- Q: Is this a behavior change? → A: No. It is a behavior-preserving restructuring. Every outcome defined by the registration verification feature and by the account change endpoints feature (005) stays exactly as it is for people and for API clients.
- Q: Should one kind of token be able to complete the other kind of action? → A: No. Registration tokens and email-change tokens stay purpose-bound, and the separation must be enforced by the shared mechanism itself so that a caller cannot forget it.
- Q: Does the email-change work need to be merged first? → A: The account change endpoints are committed on the feature branch but not merged. This work builds on them and is delivered as its own change, after they are committed.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Proving a Mailbox Works the Same Way for Every Flow (Priority: P1)

A maintainer of the identity service needs "prove you own this mailbox" to exist exactly once. A token is issued for an account and an address, mailed to that address, valid for a fixed time, replaceable by a newer one, and redeemable once. Today registration and email change each carry their own copy of those rules. After this feature, both flows rely on the same mechanism, and each flow keeps only what is specific to it: registration activates the account, and email change swaps the account's address.

**Why this priority**: This is the whole point of the feature. Everything else protects it or follows from it. Without a single home for the rules, any future fix to token lifetime, expiry, replacement, or storage has to be made twice and one copy will eventually be missed.

**Independent Test**: Register a new account and confirm it from the mailed link. Then, on an active account, request an email change and confirm it from the mailed link. Both flows must behave exactly as before, and a review of the code must show a single definition of how a proof is issued, stored, expired, replaced, and redeemed.

**Acceptance Scenarios**:

1. **Given** a new registration, **When** the person follows the mailed verification link, **Then** the account becomes active exactly as it does today, and the repeated click reports "already verified" exactly as today.
2. **Given** an active account with an accepted email-change request, **When** the person follows the mailed link, **Then** the account's email becomes the new address exactly as in feature 005, and the repeated click reports "already confirmed" exactly as today.
3. **Given** the two flows, **When** the rules for token lifetime, expiry, one outstanding proof per account, replacement, and single use are inspected, **Then** each rule is defined in exactly one place.
4. **Given** any request, response, mail subject, mail body, link path, or error code defined by features 001 to 005, **When** compared before and after this feature, **Then** nothing differs.
5. **Given** the existing automated tests for registration and for email change, **When** they are run after the change, **Then** they pass without edits to their expected outcomes, apart from tests that exist only to cover the duplicated pieces being removed.

---

### User Story 2 - A Token Can Only Complete Its Own Kind of Action (Priority: P1)

A person who receives a registration link must never be able to use that token to change an account's email, and a token mailed for an email change must never activate a registration. Today this holds because the two kinds of token are kept in separate stores. When the mechanism is shared, the separation must come from the mechanism itself, so it cannot be bypassed by a caller who forgets a check.

**Why this priority**: Merging two flows onto one mechanism is exactly where a confused-deputy hole could appear. Keeping the separation is a security requirement, not a nicety, so it ranks equal to the unification itself.

**Independent Test**: Obtain a valid registration token and present it to the email-change confirmation. Obtain a valid email-change token and present it to the registration verification. Both must be refused as invalid, and neither account may change.

**Acceptance Scenarios**:

1. **Given** a valid registration token, **When** it is presented to confirm an email change, **Then** the response is the existing `invalid_or_expired_token` outcome and no account changes.
2. **Given** a valid email-change token, **When** it is presented to verify a registration, **Then** the response is the existing `invalid_or_expired_token` outcome and no account changes.
3. **Given** the shared mechanism, **When** a caller issues or redeems a proof, **Then** the kind of proof is a required part of the request, and there is no way to issue or redeem without stating it.
4. **Given** an account with both a pending registration proof and a pending email-change proof at the same time, **When** one is replaced or discarded, **Then** the other is unaffected.

---

### User Story 3 - Account Lifecycle Still Cleans Up After Itself (Priority: P2)

When an account is closed, any pending proof for it must stop working, as it does today for a pending email change. When a person requests a second email change, the first proof must stop working. These cleanup rules must keep working across the shared mechanism, for each kind of proof independently.

**Why this priority**: These are behaviors the earlier features promised (FR-021 and FR-027 of feature 005). They are easy to lose in a restructuring because they depend on how the proofs are tracked per account.

**Independent Test**: Request an email change, close the account, then follow the mailed link: it must be refused as invalid. Request two email changes in a row, then follow the first link: it must be refused, and the second must work.

**Acceptance Scenarios**:

1. **Given** a pending email change, **When** the account is closed, **Then** the proof no longer works.
2. **Given** a pending email change, **When** the person requests a different new address, **Then** the earlier proof no longer works and the new one does.
3. **Given** a pending email change whose new address is taken by another account before confirmation, **When** the person follows the link, **Then** the confirmation is refused with the existing "account details already exist" conflict and the pending proof is discarded, exactly as in feature 005.
4. **Given** a registration that is resent, **When** the person follows the earlier link, **Then** it is refused and the newest link works, exactly as today.

---

### Edge Cases

- A registration proof issued before this change is deployed is still outstanding when the change goes live: it must keep working until it expires, or the release notes must state that outstanding registration links are invalidated. The default chosen is that they keep working (see Assumptions).
- An email-change proof issued by the unmerged feature 005 build: that feature has not been released, so no such proof exists in any environment that matters. No migration is needed for it.
- A proof is redeemed at the exact moment it expires: it is treated as expired, as today.
- Two requests redeem the same proof at the same moment: the account ends in the same state either way and nothing is applied twice, as today. Which caller hears "already used" is not changed by this feature.
- A proof exists for an account that has since been closed or is no longer in the state the flow needs: the flow's own existing handling applies (generic "invalid or expired" for the public confirm endpoints), unchanged.
- A person holds an outstanding registration proof and an outstanding email-change proof, which cannot normally coexist because email change requires an active account: if they do coexist through a data anomaly, each is still handled by its own kind and neither can complete the other's action.

## Requirements *(mandatory)*

### Functional Requirements

**One mechanism**

- **FR-001**: The system MUST have exactly one definition of how a proof of mailbox ownership is issued, stored, expires, is replaced, and is redeemed. Registration and email change MUST both use it.
- **FR-002**: The proof MUST carry the account it belongs to, the address it proves, when it was issued, when it expires, and whether and when it was redeemed.
- **FR-003**: The lifetime of a proof MUST be defined once and apply to both kinds of proof. The value MUST stay 24 hours.
- **FR-004**: An account MUST have at most one outstanding proof of each kind. Issuing a new proof of a kind MUST invalidate the earlier one of the same kind and MUST NOT affect proofs of another kind.
- **FR-005**: A proof MUST be redeemable once. Presenting an already redeemed proof MUST report that it was already used, without performing the account change a second time.
- **FR-006**: A raw token MUST never be stored. Only a non-reversible digest of it may be kept, and a proof MUST disappear on its own once it has expired.

**Separation of purpose**

- **FR-007**: Every issue, lookup, and invalidation MUST state which kind of proof it concerns, and the mechanism MUST refuse a request that does not.
- **FR-008**: A token issued for one kind MUST NOT be found, redeemed, or invalidated through the other kind.
- **FR-009**: The two kinds in scope are registration verification and email change. Adding a further kind MUST NOT require changes to how the existing kinds are stored or looked up.

**What stays specific to each flow**

- **FR-010**: Registration MUST keep its own rule for what a successful proof does: only an account awaiting verification whose email equals the proven address becomes active.
- **FR-011**: Email change MUST keep its own rules: the password and "unchanged" and "taken" checks at request time, the re-check that the address is still unused at confirmation, the conflict outcome when it is not, and the generic outcome when the account is not in a usable state.
- **FR-012**: Each flow MUST keep its own mail wording, link path, and subject, as defined by features 001 and 005.

**No observable change**

- **FR-013**: Every request, response, status code, error code, mail subject, mail body, link, and message defined by features 001 to 005 MUST remain exactly as it is.
- **FR-014**: Closing an account MUST still discard any pending email change, and MUST still end all sessions, with the same result as today.
- **FR-015**: Proofs for registration that were issued before the change MUST still be redeemable after it until their normal expiry.
- **FR-016**: No log entry, response, or error may contain a raw token or an email beyond what the existing behavior already allows (feature 005, FR-032).

**Removal of the duplicate**

- **FR-017**: The email-change flow's own separate model of a pending change, together with its own separate storage contract and storage implementation, MUST be removed. No second definition of the same concept may remain.
- **FR-018**: Tests that exist only to cover the removed duplicate MUST be removed or folded into tests of the shared mechanism, so that the shared mechanism has at least the coverage the two copies had together: issue, expiry, replacement, single use, purpose separation, and invalidation per account.

### Key Entities *(include if feature involves data)*

- **Mailbox ownership proof**: A short-lived, single-use token that ties one account to one email address and one purpose. It records when it was issued, when it expires, and when it was redeemed. It is held outside the account record and disappears on its own after expiry.
- **Proof purpose**: Which action a proof may complete. Two exist: confirming a new registration, and confirming a change of email address. The purpose is required whenever a proof is issued, found, or invalidated.
- **User account**: The account a proof belongs to. It is unchanged by this feature. Registration changes its status and email change changes its email, both only through their own existing rules.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The rules for token lifetime, expiry, replacement, single use, and storage of proofs are each defined in exactly one place, down from two.
- **SC-002**: 100% of the request and response examples, status codes, and error codes in the contracts of features 001 to 005 are unchanged after the work.
- **SC-003**: All automated tests that covered registration and email-change behavior before the change still pass with unchanged expected outcomes. Only tests covering the removed duplicate may be removed or merged.
- **SC-004**: In 100% of attempts, a registration token presented to email-change confirmation and an email-change token presented to registration verification are refused and change nothing.
- **SC-005**: A person who received a registration link before the release can still use it afterwards until it expires, in 100% of cases.
- **SC-006**: The amount of code that exists only to hold, store, and redeem an email-change proof, separate from the shared mechanism, is reduced to zero.

## Assumptions

- This is a restructuring with no change to what people or API clients see. If any required change to a response or message appears necessary during the work, it is out of scope and must be raised as a separate change.
- The account change endpoints (feature 005) are committed on the same feature branch and are not yet released, so no outstanding email-change proofs exist in any environment that must be migrated.
- Registration proofs issued before this change must keep working. Therefore the way they are already stored must stay readable by the shared mechanism, or be read from both places until they expire. Invalidating them is not acceptable by default, because it would break links already sent to people.
- The 24-hour lifetime for both kinds is retained. Making it configurable is out of scope.
- The mail delivery side (templates, senders, links) is not part of the unification. Only the proof itself is unified.
- The existing end-to-end tests for registration and for email change are the main safety net for this work and are expected to run unchanged.
- The decision recorded in feature 005's research (a separate storage family per purpose, rather than a purpose flag inside one family) is kept in spirit: the purpose is part of how a proof is found, so it cannot be skipped.
