# Feature Specification: Authentication and Sessions

**Feature Branch**: `master`
**Created**: 2026-10-11
**Status**: Draft
**Input**: `services/identity-access-next/missing-core-behaviors-part-3-authentication.txt`

## Clarifications

### Session 2026-10-11

- Q: What is the canonical name for the session model? → A: `AuthenticationSession`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sign In (Priority: P1)

A person with an active account signs in using their email and password on a device and receives credentials for continued access. The account records a session associated with that device.

**Why this priority**: Signing in is the entry point for every other authenticated action.

**Independent Test**: Start with an active account and no sessions, sign in with the correct password, and verify that one usable session and both credentials are produced. Repeat with an incorrect password and verify that no session or credentials are produced.

**Acceptance Scenarios**:

1. **Given** an active account with a matching password, **When** the account holder signs in with device information, **Then** one session is created for that account and device, and an access credential and a refresh credential are returned.
2. **Given** an active account with an incorrect password, **When** sign-in is attempted, **Then** the request is rejected and no session or credential is created.
3. **Given** a pending, suspended, or deleted account, **When** sign-in is attempted even with the correct password, **Then** the request is rejected and no session or credential is created.

---

### User Story 2 - Continue an Active Session (Priority: P1)

A signed-in person uses their refresh credential to obtain another access credential without signing in again. Their refresh credential remains the same until the session expires or is revoked.

**Why this priority**: Continued access needs a bounded session that respects account status and revocation.

**Independent Test**: Start with an active account and a valid session, refresh twice, and verify that each request returns only a new access credential while the original refresh credential stays usable.

**Acceptance Scenarios**:

1. **Given** an unexpired, unrevoked session for an active account, **When** its refresh credential is presented, **Then** a new access credential is returned, the session's last-used time is updated, and no replacement refresh credential or session is created.
2. **Given** a revoked or expired session, **When** its refresh credential is presented, **Then** no credential is issued.
3. **Given** a session whose account is no longer active, **When** its refresh credential is presented, **Then** no credential is issued.

---

### User Story 3 - End the Current Session (Priority: P2)

A signed-in person signs out of the current session using its refresh credential. Repeating sign-out is safe.

**Why this priority**: People need a reliable way to end access on a device they are using.

**Independent Test**: Revoke a valid session, verify that refreshing it fails, then repeat sign-out and verify that no further change occurs.

**Acceptance Scenarios**:

1. **Given** an unrevoked session, **When** its refresh credential is used to sign out, **Then** that session is revoked and the result reports `revoked = true`.
2. **Given** an already revoked, expired, or unknown refresh credential, **When** sign-out is attempted, **Then** the result reports `revoked = false` and no other session changes.

---

### User Story 4 - Review and End Sessions (Priority: P3)

A signed-in person reviews their sessions and, after confirming their current password, ends one selected session or all other sessions.

**Why this priority**: Device visibility and targeted sign-out help people respond to a lost or shared device.

**Independent Test**: Start with several sessions for two accounts, list the first account's sessions, revoke one of its sessions, then revoke its other sessions while confirming that the designated current session and the second account's sessions remain usable.

**Acceptance Scenarios**:

1. **Given** an authenticated account holder with several retained sessions, **When** they request their session list, **Then** only their sessions are shown, including session identity, device information, issue and expiry times, and any last-use or revocation times.
2. **Given** a correct current password and an owned, unrevoked session, **When** the account holder selects that session to end, **Then** it is revoked and the result reports `revoked = true`; a repeated request reports `false`.
3. **Given** a correct current password and a valid current session, **When** the account holder ends other sessions, **Then** every other unrevoked session belonging to that account is revoked, the current session remains usable, and the result gives the number actually revoked.
4. **Given** an incorrect current password or a session belonging to another account, **When** session revocation is requested, **Then** no session is changed.

---

### User Story 5 - End Sessions After Account Changes (Priority: P1)

When an account holder changes their password, or an account is suspended or deleted, its existing sessions stop providing access.

**Why this priority**: Old credentials must not continue to grant access after a security or account-access change.

**Independent Test**: Start with several active sessions, complete each account change in turn, and verify that every previously issued refresh credential fails afterward. Verify that a rejected change leaves sessions unchanged.

**Acceptance Scenarios**:

1. **Given** an active account with multiple sessions, **When** its password is successfully changed, **Then** all of its sessions are revoked and none of their refresh credentials can issue access.
2. **Given** an active account with multiple sessions, **When** it is suspended or deleted, **Then** all of its sessions are revoked and none of their refresh credentials can issue access.
3. **Given** an account change that is rejected, **When** the caller checks its sessions, **Then** their prior revocation states are unchanged.

### Edge Cases

- A session is expired from the instant current time reaches its expiry; refreshing it fails at that boundary.
- Empty, malformed, unknown, or unverifiable refresh credentials cannot identify a usable session and never issue access.
- Sign-in or refresh failure must not return credentials; a failed session save must not be reported as a successful sign-in.
- An account removed or made inactive after sign-in cannot refresh, even if its recorded session has not yet expired.
- Repeating revocation does not increase the number of revoked sessions. A session belonging to another account is never affected by a targeted request.
- Ending other sessions requires the designated current session to belong to the requester and remain usable; an unknown, expired, or revoked current session does not authorize that action.
- A session query shows only records still retained under the service's retention policy; it may show expired or revoked records while retained.
- A restored account does not regain sessions revoked by its earlier suspension.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The identity-access core feature MUST provide sign-in with email, password, and device information; access renewal using a refresh credential; current-session sign-out; session listing; password-confirmed targeted and other-session sign-out; and automatic revocation after successful password change, suspension, or deletion. Client-facing credential delivery, storage adapters, and additional session controls are outside this feature.
- **FR-002**: Sign-in MUST succeed only for an `ACTIVE` account whose password matches. An absent, inactive, or password-mismatched account MUST receive no credential and create no session.
- **FR-003**: Each successful sign-in MUST create exactly one bounded `AuthenticationSession` associated with the account and supplied device information, then return one access credential and one refresh credential. Success MUST be reported only after the session is recorded.
- **FR-004**: A session MUST have a unique session identity, account identity, device information, issue time, expiry time, optional last-use time, and optional revocation time. Its expiry MUST be later than its issue time, and expiry MUST begin when current time reaches that time.
- **FR-005**: A refresh credential MUST have at least 128 bits of unpredictability, identify only its corresponding session, and MUST NOT be stored in readable form or exposed through session views. The credential remains unchanged for that session until expiry or revocation; refresh MUST NOT rotate it or create another session.
- **FR-006**: Renewal MUST issue only a new access credential when the presented refresh credential resolves to an unrevoked, unexpired session for an `ACTIVE` account. On success it MUST record that session's last-use time. Otherwise it MUST issue no credential and leave other sessions unchanged.
- **FR-007**: Current-session sign-out MUST resolve the presented refresh credential and revoke its matching usable session at most once. It MUST return `revoked = true` only when it newly revokes the session; unknown, expired, or already revoked credentials MUST return `false`.
- **FR-008**: Session listing MUST use the authenticated account identity, return only that account's retained sessions, and show each session's identity, account identity, device information, issue and expiry times, and any last-use and revocation times. It MUST NOT reveal refresh credentials.
- **FR-009**: Targeted sign-out MUST require the requesting account's correct current password and an owned session identity. It MUST newly revoke only that session and report whether a revocation occurred; an unknown, non-owned, or already revoked session MUST not be reported as newly revoked.
- **FR-010**: Other-session sign-out MUST require the requesting account's correct current password and an owned, usable current session identity. It MUST revoke every other unrevoked session for that account, leave the current session usable, and return the number of sessions newly revoked.
- **FR-011**: An incorrect current password or unauthorized session identity MUST cause targeted or other-session sign-out to change zero sessions.
- **FR-012**: A successful password change, suspension, or deletion MUST revoke every session for that account before the account change is reported as successful. Rejected changes MUST leave sessions unchanged. Reinstatement MUST NOT restore revoked sessions.
- **FR-013**: Access credentials MUST represent the authenticated account and its email at issuance. A failed sign-in or renewal MUST never issue an access credential.
- **FR-014**: Session behavior MUST use the same account accessibility rule as other protected account actions: only `ACTIVE` accounts can sign in, renew access, or manage their sessions.
- **FR-015**: The feature MUST NOT add refresh-credential rotation, generations, grace periods, or replay-detection behavior.

### Key Entities *(include if feature involves data)*

- **User account**: Existing account identified by user ID, email, password verification data, and status. Only an `ACTIVE` account is accessible for authentication.
- **AuthenticationSession**: A bounded, revocable record of one account's continued access from one device. It has a unique session ID, owner, device information, issue and expiry times, and optional last-use and revocation times.
- **Refresh credential**: A private, unpredictable proof linked to one authentication session. It can renew access while both the session and its account remain eligible.
- **Session view**: The account holder's safe representation of a retained session, excluding credentials.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In acceptance tests, 100% of valid sign-ins create exactly one session and return both credentials; 100% of invalid-password and inactive-account attempts create zero sessions and return zero credentials.
- **SC-002**: For every valid session in acceptance tests, two consecutive renewals each return one access credential, return zero replacement refresh credentials, and preserve the same session; every expired, revoked, or inactive-account case returns zero credentials.
- **SC-003**: Current-session and targeted sign-out revoke exactly one eligible session on first request and zero on repeat requests. Other-session sign-out reports exactly the number of newly revoked sessions and leaves the designated current session usable in 100% of acceptance cases.
- **SC-004**: A signed-in person with 100 retained sessions can see all 100 of their own sessions and zero sessions belonging to another account, including the required timestamps and device information.
- **SC-005**: After a successful password change, suspension, or deletion, 100% of the account's previously issued refresh credentials fail to renew access; rejected changes revoke zero sessions.
- **SC-006**: In a standard environment with responsive account and session storage, at least 95% of eligible sign-in and renewal attempts return their result within 2 seconds.

## Assumptions

- The existing user-account model and password-matching behavior from Part 1 supply the account eligibility and password checks needed here. Pending verification, suspended, and deleted accounts are not `ACTIVE`.
- Session lifetime and retention are set by service policy outside this brief. The lifetime is finite; records may remain visible after expiry or revocation while retained.
- Last-use time is initially absent and changes after a successful renewal. Issuing a session at sign-in does not count as renewal.
- The requesting account identity for session listing and session management comes from the authenticated caller; a supplied account ID is not by itself proof of ownership.
- `AuthenticationSession` is the canonical domain name for the record called `RefreshSession` in the source brief. The source brief remains an input to implementation planning.
