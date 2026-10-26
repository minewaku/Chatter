# Feature Specification: Session Endpoint Error Causes

**Feature Branch**: `identity-access-next`

**Created**: 2026-10-26

**Status**: Draft

**Input**: User description: "Replace the presentation-layer exception wrappers with specific core exceptions on the session-management endpoints." A review of the session endpoints (list sessions, end one session, end all other sessions) found that the service reports every failure on them as one generic "authentication failed" outcome, and the web layer then guesses the real cause from which endpoint was called. The same rule for confirming a current password is also written twice, once for account changes and once for session management, with different outcomes. This feature makes the service report the real cause, keeps one rule for confirming a current password, and removes the web layer's guessing. It amends the error behavior chosen in spec 004 (research R4) and part of the spec 002 contract.

## Clarifications

### Session 2026-10-26

- Q: Should every account that is not active (pending verification, locked, suspended, deleted) produce the same "account cannot perform this action" answer? → A: Yes, as stated in the request. Only an active account is accessible; the four other statuses share one answer. Telling them apart is a possible later change and is not part of this feature. There is no limited-but-can-sign-in status.
- Q: What status should an unusable current session (expired or not owned) get on "end other sessions"? → A: `401`, with its own session-specific code distinct from `authentication_failed` and `reauthentication_failed`. A client that receives it treats the session as dead and renews or signs out. The code name is chosen in planning.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A Client Can Tell A Wrong Password From A Blocked Account (Priority: P1)

A person is signed in on a device and asks to end one of their sessions, or all their other sessions, confirming with their current password. If the password is wrong, the client should say so and let them retry. If instead the account has been locked or suspended since the access credential was issued, retrying is pointless and the client should explain that the account cannot perform this action. Today both situations get the same answer on these endpoints, so a person whose account was blocked is told their password is wrong and keeps retrying.

**Why this priority**: This is the user-visible defect. Everything else in this feature exists to make this distinction reliable and to remove the guesswork behind it.

**Independent Test**: With a signed-in caller, call both password-confirmed session actions with a wrong password and then, after the account is blocked, with the right password. Verify the first gets the "request could not be confirmed" answer and the second gets the "account cannot perform this action" answer.

**Acceptance Scenarios**:

1. **Given** an active account and a valid access credential, **When** the caller ends one session or all other sessions with a wrong current password, **Then** the answer is `403` with code `reauthentication_failed`, and the attempt counts toward the password-confirmation limit.
2. **Given** an account that is locked, suspended, or pending verification while the caller still holds a valid access credential, **When** the caller ends one session or all other sessions with the correct password, **Then** the answer is `403` with code `account_inaccessible`.
3. **Given** the same blocked account, **When** the caller supplies a wrong password, **Then** the answer is still `account_inaccessible`: the account's status is reported first and the password is not compared, so the answer never hints at whether the password was right.
4. **Given** a current password that does not meet the password rules (for example, too short), **When** the caller submits it, **Then** it is treated as a wrong password (`reauthentication_failed`), not as a malformed request.
5. **Given** an access credential whose account no longer exists, **When** the caller ends a session, **Then** the answer is `account_inaccessible`.
6. **Given** the password-confirmation limit has been reached, **When** the caller tries again, **Then** the answer is the existing "too many attempts" answer, unchanged.

---

### User Story 2 - Listing Sessions Reports The Same Cause (Priority: P1)

A signed-in person opens their session list. If their account is no longer accessible, the answer says so in the same terms as above.

**Why this priority**: It is the second of the three session endpoints and uses the same account rule. Without it the cause would still be reported differently depending on the endpoint.

**Independent Test**: Block an account that has a valid access credential, then list sessions. Verify the answer is `account_inaccessible`. List sessions for an active account and verify the list is returned as before.

**Acceptance Scenarios**:

1. **Given** an account that is locked, suspended, pending verification, or deleted but whose caller holds an access credential that is still accepted, **When** the caller lists sessions, **Then** the answer is `403` with code `account_inaccessible`.
2. **Given** an access credential whose account no longer exists, **When** the caller lists sessions, **Then** the answer is `account_inaccessible`.
3. **Given** an active account, **When** the caller lists sessions, **Then** the list is returned exactly as today.

---

### User Story 3 - One Rule For Confirming The Current Password (Priority: P1)

Changing a password, changing an email, closing an account, ending a session, and ending other sessions all ask the person to confirm their current password. All five should follow the same rule: first check the account is active, then check the password, and report which of the two failed. Today the account-change actions follow this rule and the session actions follow a looser one, so the same blocked account gets a different answer depending on which action it tries.

**Why this priority**: A single rule is what keeps Stories 1 and 2 from drifting apart again, and it is the reason the web layer has been guessing.

**Independent Test**: For each of the five password-confirmed actions, use an active account with a wrong password, and a blocked account with the right password. Verify all five give `reauthentication_failed` for the first and `account_inaccessible` for the second (closing an account keeps its existing rule that any status may close).

**Acceptance Scenarios**:

1. **Given** an active account, **When** any password-confirmed action receives a wrong password, **Then** it is refused with `reauthentication_failed`.
2. **Given** a non-active account, **When** a password-confirmed action other than closing the account receives the correct password, **Then** it is refused with `account_inaccessible`.
3. **Given** a wrong password on a non-active account, **When** the action is attempted, **Then** the password is not compared and the answer is `account_inaccessible`.
4. **Given** repeated wrong passwords, **When** the limit is reached, **Then** the shared password-confirmation limit applies across all five actions, as today.
5. **Given** a refused attempt because the account is not accessible, **When** the attempt count is updated, **Then** the count is cleared as it is for any non-password outcome and is not increased.

---

### User Story 4 - Sign-In And Renewal Stay Generic (Priority: P2)

Sign-in and access renewal are the entry points an attacker can probe without being signed in. They keep giving one generic "authentication failed" answer (`401`, code `authentication_failed`) that never reveals whether an account, password, or session exists. That generic answer is now used only there.

**Why this priority**: It protects the surfaces where it matters and keeps this feature from weakening them. No behavior changes, so it is a regression guard.

**Independent Test**: Sign in with an unknown email, a wrong password, a blocked account, and an invalid email format; renew with an unknown, expired, and malformed refresh credential. Verify all get the same `401` answer as before.

**Acceptance Scenarios**:

1. **Given** any failed sign-in (unknown email, wrong password, non-active account, malformed input), **When** it is refused, **Then** the answer is `401` with code `authentication_failed`, identical in every case.
2. **Given** any failed renewal (blank, unknown, reused, expired, or unusable refresh credential, or an inaccessible owner), **When** it is refused, **Then** the answer is the same `401` answer as today.
3. **Given** the session endpoints, **When** they fail, **Then** none of them ever answers `authentication_failed`.

---

### User Story 5 - An Unusable Current Session Has Its Own Answer (Priority: P3)

When a person asks to end all their other sessions, the service needs the session they are calling from to still be usable. If it is not (it expired or does not belong to them) the service refuses with an answer specific to that situation, not the answer for a wrong password. This is rare, because revoked sessions are already rejected before reaching this point, so it arises only from timing races or expiry.

**Why this priority**: It is a rare edge, but without its own answer it would be reported as a wrong password and send the person into a retry loop.

**Independent Test**: Call "end other sessions" with a valid access credential whose session has just expired. Verify the answer is `401` with the session-specific code, not `reauthentication_failed`, and that no other session is ended.

**Acceptance Scenarios**:

1. **Given** a caller whose current session is expired or not owned by them, **When** they end all other sessions with the correct password, **Then** the answer is `401` with the session-specific code and no other session is revoked.
2. **Given** the same situation with a wrong password, **When** the request is made, **Then** the wrong password is reported first, since the password is checked before the session.

---

### Edge Cases

- **Account status changes between access credential issue and use**: locking an account does not revoke its sessions, so a locked account's still-valid access credential reaches these endpoints and gets `account_inaccessible`. Suspending or deleting an account revokes its sessions, so those callers are usually rejected earlier with `401`; only a short race lets them through. A pending-verification account cannot sign in, so it normally holds no access credential. The other reachable case is an account whose record has disappeared.
- **Closing an account**: unchanged. Any status may close, so closing keeps its existing rule of checking only the password.
- **A deleted account** is reported as `account_inaccessible` where it is reachable; it is not a separate answer.
- **Password-confirmation limit and blocked accounts**: a caller cannot use a blocked account to probe the password, because the password is never compared.
- **Error body**: every answer still uses the existing single error shape; only the code and status differ as described.
- **Dependency outage** (session store, rate-limit store): unchanged; those answers (`503`) are not affected.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The service MUST report the actual cause when a password-confirmed session action or the session list is refused: wrong password, account missing or not accessible, or current session unusable. It MUST NOT collapse these into one generic outcome on these endpoints.
- **FR-002**: Ending one session and ending all other sessions MUST answer `403` with code `reauthentication_failed` when the current password is wrong, including a password that fails the password rules.
- **FR-003**: Those two actions and the session list MUST answer `403` with code `account_inaccessible` when the account does not exist or its status is anything other than active (pending verification, locked, suspended, deleted).
- **FR-004**: When the account is not accessible, the answer MUST be `account_inaccessible` regardless of whether the supplied password is right or wrong, and the password MUST NOT be compared.
- **FR-005**: Confirming a current password MUST follow one rule in every action that asks for it (change password, change email, close account, end one session, end other sessions): check the account first (except where an action already allows any status, i.e. closing), then check the password, and report which one failed. The two failures MUST be distinguishable by callers.
- **FR-006**: The presentation layer MUST NOT inspect the endpoint, or any context other than the reported cause, to decide which error to return. It MUST map each reported cause to its answer directly.
- **FR-007**: The generic `authentication_failed` answer (`401`) MUST be produced only by sign-in and access renewal, with unchanged behavior and unchanged answers for every failure on those two.
- **FR-008**: The shared password-confirmation limit MUST count an attempt only when the password was wrong. A refusal because the account is not accessible, a missing account, or an unusable session MUST clear the count like any other non-password outcome. The limit's size, window, and `429` answer MUST NOT change.
- **FR-009**: Ending all other sessions MUST refuse with `401` and a dedicated session-specific code (distinct from `authentication_failed`) when the caller's current session is expired or not owned by the caller, and MUST NOT revoke any session in that case. The wrong-password check comes first.
- **FR-010**: The `account_inaccessible` answer MUST be a single answer for all non-active statuses. The service MUST NOT expose which status the account has.
- **FR-011**: All five password-confirmed actions and the session list MUST list `account_inaccessible` (and `reauthentication_failed` where a password is confirmed) among their documented error responses; the two session actions MUST also document the session-specific answer.
- **FR-012**: The two web-layer-only error types that wrap the generic failure MUST NOT exist after this feature, and the controllers for the three session endpoints MUST NOT translate one error type into another.
- **FR-013**: Spec 004 research R4 MUST be marked as superseded by this feature, and the error descriptions in the spec 007 HTTP surface contract and the spec 002 contract wording MUST be updated to match this behavior.
- **FR-014**: This feature MUST NOT change the account status model, which statuses are accessible, which endpoints exist, who may call them, session lifetime and revocation rules, or any non-error response.

### Key Entities

- **User Account**: The person's account with a status (pending verification, active, locked, suspended, deleted). Only active is accessible. Owns the rule for confirming a current password.
- **Authentication Session**: One sign-in for one device. Its usability (not expired, not revoked, owned by the caller) is what the session-specific answer reports.
- **Error Answer**: The status and code returned to the caller. This feature fixes which cause maps to which answer for the session endpoints and the other password-confirmed actions.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For each of the two password-confirmed session actions, 100% of wrong-password attempts on an active account return `reauthentication_failed` and 100% of attempts on a non-active or missing account return `account_inaccessible`, whatever password was sent.
- **SC-002**: 100% of session-list requests for a non-active or missing account return `account_inaccessible`; 0% return `authentication_failed`.
- **SC-003**: Across the five password-confirmed actions, the answer for the same account and password input is identical in 100% of comparisons (except closing an account, which keeps its existing any-status rule).
- **SC-004**: 100% of failed sign-ins and renewals return exactly the same status, code, and body as before this feature.
- **SC-005**: 0% of attempts against a non-active account increase the password-confirmation count.
- **SC-006**: No controller on the three session endpoints contains error translation logic, and the two web-layer-only wrapper types are gone, verified by inspection of the final code.
- **SC-007**: Every document that described the old behavior (spec 004 R4, spec 007 HTTP surface contract, spec 002 contract wording) states the new behavior or points to this feature, with 0 contradictions remaining.

## Assumptions

- **Only the three session endpoints and the shared confirmation rule change.** Password change, email change, and account close already report wrong password and inaccessible account separately; they only need to share the one rule.
- **Closing an account keeps its any-status behavior** (it checks only the password), as in the current model.
- **The new "account missing or not accessible" outcome sits beside the existing "operation not allowed in this account state" outcome.** Both reach callers as `account_inaccessible`. Merging them is an internal clean-up for later.
- **Telling a wrong password from a blocked account is an accepted disclosure.** The caller is already authenticated by a valid access credential, and the account-change endpoints already make this distinction. The account's status is checked first so a blocked account never reveals whether a password is right.
- **Account statuses are not distinguished to callers.** Pending verification, locked, suspended, and deleted all give `account_inaccessible`. Separate codes per status can be added later.
- **Existing access credentials keep working as before.** The revoked-session check that rejects credentials of revoked sessions with `401` is unchanged, so suspended and deleted accounts usually never reach these endpoints.
- **Dependencies**: relies on the existing password-confirmation limit, the existing error shape and code list, and the existing revoked-session check.
- **Open item for a possible follow-up, not required here**: locking an account does not revoke its sessions (suspending does), so a locked account's access credentials stay valid until they expire and only renewal fails. Whether lock should revoke sessions too is a separate decision.
- **Out of scope**: changing the current-session sign-out, email-change confirmation, or how sessions are ended beyond using the shared rule; new lock or suspend endpoints; any change to the account status model.
- **Unusable current session answers `401`** with a dedicated code (see Clarifications). It is not `403`, so it is never mistaken for a wrong password or a blocked account; clients that treat `401` as "renew or sign in again" do the right thing for a dead session.
