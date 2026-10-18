# Feature Specification: Account Change Endpoints

**Feature Branch**: `identity-access-next`

**Created**: 2026-10-17

**Status**: Draft

**Input**: User description: "There're some features left unimplemented: changeEmail, changeUsername that I dont see it apis in `identityaccess-presentation` package of `identity-access-next` service, and I think that the `close` api (which soft-delete user) should came with password confirmation"

## Clarifications

### Session 2026-10-17

- Q: How should the HTTP method for closing an account be chosen, and what happens on a wrong password? → A: The method is free to change so the password can travel in the request body. A wrong password must be refused without signing the person out.
- Q: Does changing the username need the current password? → A: No. A username is a display name with no security role, so the change needs only a signed-in person.
- Q: Which email is in effect before a new address is verified? → A: The old address stays in effect (sign-in, mail) until the new address is verified. The new address is held as a pending change. Changing the email needs the current password and a verification email to the new address.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Close My Account Only With My Password (Priority: P1)

A signed-in person closes their own account. Closing is destructive and the person cannot undo it, so it must be confirmed with the current password, the same way changing the password already is. A stolen or left-open access credential alone must not be enough to destroy an account. Mistyping the password must be a recoverable mistake: it is refused, and the person stays signed in.

**Why this priority**: Today the account can be closed with nothing but a valid access credential. That is a security gap in an existing feature, so it comes before any new capability.

**Independent Test**: Sign in, try to close the account with a wrong password, with no password, and with the correct password. Confirm only the last one closes the account and ends every session, and that after the wrong attempt the same access credential still works.

**Acceptance Scenarios**:

1. **Given** a signed-in person with an active account, **When** they close the account with the correct current password, **Then** the account is closed, every session of the account is ended, and the response reports success with no body.
2. **Given** a signed-in person, **When** they close the account with a wrong current password, **Then** the response is the "refused, could not be confirmed" outcome used for other password-confirmed actions (not "unauthenticated"), the account stays open, and their session and credentials remain valid.
3. **Given** a signed-in person, **When** they close the account without supplying a password, or with a blank one, **Then** the response is a validation failure, the account stays open, and their session remains valid.
4. **Given** an account that is already closed, **When** the same person's old access credential is used to close it again, **Then** the credential is refused as "unauthenticated" because closing ended all its sessions, and nothing about the account is revealed.
5. **Given** a closed account, **When** anyone signs in with its former email, username, or password, **Then** sign-in fails with the generic outcome and the former email and username are no longer attached to the account.

---

### User Story 2 - Change My Username (Priority: P2)

A signed-in person picks a new username. A username is a display name and plays no part in signing in, so no password is needed. The new username follows the same rules as at registration and must not already belong to another account.

**Why this priority**: A username is a visible identity people want to fix after registering. It carries no sign-in or recovery weight, so it is low risk and simple.

**Independent Test**: Sign in, change the username, and confirm the account now reports the new username while the old one can be registered by someone else.

**Acceptance Scenarios**:

1. **Given** a signed-in person with an active account, **When** they submit a valid, unused new username, **Then** the username changes and the response reports success with no body.
2. **Given** the same request, **When** the new username equals the current one, **Then** the outcome is a distinct "nothing changed" conflict, like the existing unchanged-password outcome, and nothing is written.
3. **Given** a new username already used by another account, **When** the person submits it, **Then** the response is a conflict reporting that the account details already exist, without saying which account holds it.
4. **Given** a new username that breaks the registration rules, **When** the person submits it, **Then** the response is a validation failure listing the field, with no submitted value echoed back.
5. **Given** a suspended, locked, or pending-verification account, **When** the person tries to change the username, **Then** the response is the "account cannot perform this action" refusal.
6. **Given** no valid access credential, **When** anyone tries to change a username, **Then** the response is "unauthenticated".

---

### User Story 3 - Change My Email Address (Priority: P2)

A signed-in person moves their account to a new email address. They confirm with their current password, and the service sends a verification email to the new address. Until the person proves they own the new address, nothing about the account changes: sign-in and all mail keep using the old address. Once the new address is verified, it replaces the old one.

**Why this priority**: People change addresses, and today they have no way to do it. It is higher risk than a username change because the email is the sign-in identifier and the target for verification mail, so it needs both the password and proof of ownership.

**Independent Test**: Sign in, request an email change with the correct password, confirm that sign-in with the old address still works and the new one does not, follow the verification for the new address, and confirm sign-in now works with the new address and no longer with the old one.

**Acceptance Scenarios**:

1. **Given** a signed-in person with an active account, **When** they submit a valid, unused new email with the correct current password, **Then** the response reports the request was accepted with no body, a verification email is sent to the new address, and the account's email is still the old one.
2. **Given** an accepted, unconfirmed email change, **When** the person signs in, **Then** the old address and password work, and the new address does not.
3. **Given** an accepted, unconfirmed email change, **When** the person follows the valid verification token for the new address, **Then** the account's email becomes the new one, sign-in works with it and no longer with the old one, and repeating the same confirmation does not fail.
4. **Given** an accepted, unconfirmed email change, **When** the person requests a different new email, **Then** the earlier pending change is replaced and its verification token stops working.
5. **Given** a blank, unknown, used, replaced, or expired verification token, **When** it is presented to confirm an email change, **Then** the response is the existing `invalid_or_expired_token` outcome and nothing changes.
6. **Given** a pending change, **When** another account takes the new address before it is confirmed, **Then** confirming is refused with the "account details already exist" conflict and the pending change is dropped.
7. **Given** a new email equal to the current one, **When** the person submits it, **Then** the outcome is the "nothing changed" conflict, nothing is stored, and no email is sent.
8. **Given** a new email already used by another account, **When** the person submits it, **Then** the response is the "account details already exist" conflict, without revealing whose account it is, and no email is sent.
9. **Given** a new email that is not a valid address, **When** the person submits it, **Then** the response is a validation failure listing the field, with no submitted value echoed back.
10. **Given** a wrong current password, **When** the person submits a valid new email, **Then** the response is the "could not be confirmed" refusal, nothing is stored or sent, and their session remains valid.
11. **Given** a suspended, locked, or pending-verification account, **When** the person tries to change the email, **Then** the response is the "account cannot perform this action" refusal.

---

### Edge Cases

- A person submits the same email change twice in a row: the second request replaces the first pending change, so only the newest verification token works. This is not an error.
- Two people race to take the same username or email: exactly one succeeds, the other gets the "already exist" conflict.
- The account is closed while an email change is pending: the pending change is discarded and its verification token no longer works.
- The person's password is changed while an email change is pending: the pending change stays valid. The password confirmed the request, and the proof of the new address is the token.
- A verification token for an email change is presented where a registration token is expected, or the other way round: it is refused as invalid. Each kind of token only completes its own action.
- Repeated wrong-password attempts on the password-confirmed actions: the existing protection for password-confirmed actions applies unchanged. This feature adds no new throttling and never ends a session because of a wrong password.
- An access credential issued before an email or username change still carries the old email claim until it expires. Nothing about the change may be derived from the credential, only from the stored account.
- The new username or email differs from an existing one only by letter case or surrounding spaces: it is treated as the same value under the registration rules.

## Requirements *(mandatory)*

### Functional Requirements

**Close account**

- **FR-001**: Closing an account MUST require the caller's current password in the request.
- **FR-002**: A wrong current password MUST be refused with the same outcome used for other password-confirmed actions, MUST NOT be reported as "unauthenticated", and MUST leave the account untouched.
- **FR-003**: A wrong, missing, or blank password MUST NOT end the caller's session, revoke any credential, or otherwise affect the caller's ability to keep using the service.
- **FR-004**: A missing or blank password MUST be rejected as a validation failure before any account lookup.
- **FR-005**: A successful close MUST end all of the account's sessions, with the same immediate effect on already-issued access credentials as the other session-ending paths.
- **FR-006**: The current password MUST NOT travel in the address of the request, so it cannot end up in access logs, browser history, or proxy logs. The request method may change to meet this.
- **FR-007**: The target of the close MUST be the account named by the validated access credential. No client-supplied identifier is accepted.
- **FR-008**: The previous behavior of closing without a password MUST be removed, not kept as an alternative. Clients of the earlier version are expected to move to the confirmed form.

**Change username**

- **FR-009**: A signed-in person MUST be able to change their own username by supplying the new username, with no password.
- **FR-010**: The new username MUST satisfy the same rules as at registration, and a violation MUST be reported as a validation failure naming the field and no submitted value.
- **FR-011**: A new username already used by another account MUST be rejected with the existing "account already exists" conflict and MUST NOT reveal who holds it.
- **FR-012**: A new username equal to the current one MUST be reported as a "nothing changed" conflict and MUST write nothing.
- **FR-013**: The change MUST be refused with the "account cannot perform this action" outcome unless the account is active.
- **FR-014**: On success the response MUST have no body and the stored username MUST be the new one for every later read.

**Change email**

- **FR-015**: A signed-in person MUST be able to request a change of their own email by supplying the new email and the current password.
- **FR-016**: The new email MUST satisfy the same rules as at registration, and a violation MUST be reported as a validation failure naming the field and no submitted value.
- **FR-017**: A new email already used by another account MUST be rejected with the existing "account already exists" conflict and MUST NOT reveal who holds it.
- **FR-018**: A new email equal to the current one MUST be reported as a "nothing changed" conflict, MUST write nothing, and MUST NOT send mail.
- **FR-019**: The request MUST be refused with the "account cannot perform this action" outcome unless the account is active.
- **FR-020**: An accepted request MUST store a pending change tied to the account and send a verification email to the new address only. The account's email, sign-in identifier, and the destination of all other mail MUST remain the old address until the change is confirmed.
- **FR-021**: An account MUST have at most one pending email change. A new request MUST replace the earlier one, and the earlier verification token MUST stop working.
- **FR-022**: Presenting the valid verification token MUST replace the account's email with the pending one, discard the pending change, and make the token unusable for a second time without error to the caller.
- **FR-023**: Confirmation MUST NOT require the person to be signed in, because the link is opened from the new mailbox, possibly on another device. The token alone proves ownership of the new address.
- **FR-024**: Confirmation MUST re-check that the new address is still unused. If another account took it, confirmation MUST be refused with the "account details already exist" conflict and the pending change MUST be discarded.
- **FR-025**: A blank, unknown, used, replaced, or expired token MUST yield the existing `invalid_or_expired_token` outcome and change nothing.
- **FR-026**: The verification token for an email change MUST be valid only for that purpose and MUST have the same lifetime as existing verification tokens. A registration token MUST NOT complete an email change, and an email-change token MUST NOT activate a registration.
- **FR-027**: Closing the account MUST discard any pending email change.
- **FR-028**: The request itself MUST respond the same way (accepted, no body) whether or not the new address could receive mail, and MUST NOT reveal anything beyond the conflicts defined above.

**Shared behavior**

- **FR-029**: Change username, change email, and close MUST take the target account only from the validated access credential and MUST accept no client-supplied user identifier. Confirmation of an email change is the one exception: it is identified by its token alone (FR-023).
- **FR-030**: Change username, change email, and close MUST be protected: a missing, invalid, expired, or revoked access credential MUST yield the existing "unauthenticated" outcome.
- **FR-031**: All new outcomes MUST use the existing error format and stable codes. Any new code introduced for "nothing changed" MUST follow the same lowercase identifier convention as `password_unchanged`.
- **FR-032**: No log entry, response, or error may contain the current password, a verification token, or the old or new email or username beyond what a success response intentionally returns.
- **FR-033**: Changing a username or an email MUST NOT end the person's sessions. Only closing the account and changing the password do that.
- **FR-034**: The published API documentation MUST describe every new or changed action, its request shape, and every outcome above, with password fields marked write-only.
- **FR-035**: Each action MUST have automated tests covering success, wrong password where a password is required, validation failure, duplicate, unchanged, inaccessible account, and missing credential. Tests for email-change confirmation MUST cover token states using a captured outgoing message, not a real mailbox. The structure checks that already guard the presentation module MUST keep passing.

### Key Entities *(include if feature involves data)*

- **Account**: The person's identity in the service, with a username, an email, a status (pending verification, active, suspended, locked, or closed), and a password. Closing replaces the email and username with unusable placeholders and the password with an unusable one, so the originals can be reused by others.
- **Pending email change**: A new address awaiting proof of ownership. Tied to exactly one account and one verification token, at most one per account, and discarded on confirmation, replacement, expiry, conflict, or account closure.
- **Verification token**: The existing single-use, expiring proof sent by email. A new purpose, "confirm a new email address", is added alongside the existing "confirm a registration" purpose.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: No request that closes an account succeeds without the correct current password: across all tested wrong, missing, and blank passwords, 100% leave the account open.
- **SC-002**: After any wrong, missing, or blank password on close or email change, 100% of the caller's credentials still work on the next request.
- **SC-003**: After a successful close, 100% of previously issued access credentials and refresh credentials of that account are refused on the next request.
- **SC-004**: A person can change their username in a single request, with no other step, and see the new value on the next read.
- **SC-005**: Between an accepted email-change request and its confirmation, 100% of sign-in attempts and account mail still use the old address, and from confirmation on, 100% use the new one.
- **SC-006**: A person can complete the whole email change, including verifying the new address, in under 3 minutes of their own time.
- **SC-007**: Duplicate username and email attempts return the same outcome regardless of which account holds the value, so no response lets a caller discover who holds a value beyond the behavior registration already has.
- **SC-008**: In logs and responses captured during the automated tests of all actions, there are 0 occurrences of a submitted password or a verification token.

## Assumptions

- The target audience is the existing signed-in clients of the identity-access-next service. The earlier close-without-password form has not been released to outside clients, so replacing it rather than versioning it is acceptable.
- The core already has change-email, change-username, and close behaviors. Core must change to match this spec: username change drops its password check, and email change becomes a pending change that is applied on token confirmation instead of being applied at once.
- Password confirmation uses the same rules and the same outcomes as the existing password change, including "wrong password is a refusal, not 'unauthenticated'".
- Usernames remain unique across accounts, as at registration, even though they are only display names.
- Only the account's owner acts on their own account here. Administrator-driven changes, suspension, locking, and reinstating are out of scope.
- A closed account stays closed. Reopening is out of scope.
- No new rate limiting is introduced. Existing protection for password-confirmed actions is relied on.
- A notice to the old address that an email change was requested is out of scope for this feature.
- Sessions are not ended by a username or email change. A person who suspects takeover is expected to change the password, which already ends all sessions.
- Access credentials keep their issue-time email claim until they expire. Clients that need the current address read it from the account, not from the credential. After confirmation, new credentials carry the new address.
- The existing verification email mechanism, token format, and public base address are reused for the new address. Building a separate channel is out of scope.
- Real delivery to a real inbox is checked by the owner by hand and is not covered by automated tests. Automated tests only check what the service sends (recipient, link, token) through a captured outgoing message, and what happens when the token is presented.
- Display name, birthday changes, and avatar are out of scope.
