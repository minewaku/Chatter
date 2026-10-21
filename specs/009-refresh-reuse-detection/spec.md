# Feature Specification: Refresh Credential Reuse Detection

**Feature Branch**: `identity-access-next`

**Created**: 2026-10-21

**Status**: Draft

**Input**: User description: "Read this conversation as input" — a walkthrough of how `identity-access-next` signs people in, renews access, and signs them out. It established that each sign-in is one session, that renewal replaces the session's refresh credential with a new one and makes the old one unusable, and that signing a session out immediately stops its access credentials from working. It then listed possible improvements and ranked one first: today, presenting an already-replaced refresh credential is refused, but nothing else happens, so someone who stole a credential and replays it after the real owner has renewed is never noticed and the session stays alive. This feature closes that gap. The other improvements discussed (restructuring how revoked access credentials are checked, removing duplicated session rules, and tightening session-store settings) are separate pieces of work and are not part of this feature.

## Clarifications

### Session 2026-10-21

- Q: If two renewal requests arrive at the same moment with the same still-current refresh credential, should the request that loses the race end the session? → A: No. The losing request is refused but the session is not revoked, as long as it overlapped in time with the winning renewal. Any presentation of the replaced credential after the winning renewal has completed is reuse and revokes the session.
- Q: Besides renewal, current-session sign-out also accepts a refresh credential; should signing out with an already-replaced credential count as reuse and revoke the session? → A: No. Reuse detection applies to renewal only. Sign-out with a replaced credential behaves as today: it reports `revoked = false` and changes no session.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Replaying A Replaced Refresh Credential Ends The Session (Priority: P1)

A refresh credential can be copied by someone other than its owner (a leaked log, a compromised device, an intercepted request). When the real owner renews, the credential is replaced and the copy stops working, which is good, but the session itself keeps going. If the thief then presents the old copy, the request is simply refused and everyone carries on. The owner has no sign that anything happened, and if the thief had renewed first, the thief now holds the live credential. A replaced credential coming back is the clearest sign that two parties hold the same session, so the session is ended: it is revoked, its refresh credential stops working, and its access credentials stop working immediately.

**Why this priority**: This is the whole feature. Without it, a stolen refresh credential gives an attacker a quiet foothold that lasts as long as the session can be renewed, which has no upper limit.

**Independent Test**: Sign in, renew once so the first refresh credential is replaced, then present the first credential again. Verify the request is refused, the session is revoked, the credential issued by the renewal no longer renews access, and an access credential from this session is rejected on its next use.

**Acceptance Scenarios**:

1. **Given** a session whose refresh credential has been replaced by a renewal, **When** the replaced credential is presented, **Then** no credential is issued, the session is revoked, and the person gets the same refusal as for any other unusable refresh credential.
2. **Given** that session was revoked by reuse, **When** the replacement refresh credential (the one issued by the renewal) is presented, **Then** no credential is issued.
3. **Given** that session was revoked by reuse, **When** an access credential issued for it is presented to a protected endpoint, **Then** the request is refused as for any signed-out session.
4. **Given** the same person has other sessions on other devices, **When** one session is revoked by reuse, **Then** the other sessions are unchanged and keep working.
5. **Given** a session renewed several times, **When** any of its earlier replaced credentials (not only the most recent) is presented for renewal, **Then** the session is revoked in the same way.
6. **Given** a session whose refresh credential has been replaced, **When** the replaced credential is used to sign out the current session, **Then** the result reports `revoked = false`, no session changes, and no reuse event is recorded.

---

### User Story 2 - Normal Use Is Unaffected (Priority: P1)

People who renew in the ordinary way must not be signed out by the new check. A refresh credential used once, in order, behaves exactly as today: it is replaced, the session gets a full new lifetime, and the new credential works.

**Why this priority**: A security check that signs out honest users trains people to distrust it and generates support load. It is the same priority as the detection itself.

**Independent Test**: Sign in and renew repeatedly, always presenting the most recently issued credential. Verify every renewal succeeds, the session is never revoked, and the session lifetime behaves as before.

**Acceptance Scenarios**:

1. **Given** a usable session, **When** its current refresh credential is presented, **Then** access is renewed, the credential is replaced, and the session is not revoked.
2. **Given** a session that has been renewed many times in order, **When** the current credential is presented again, **Then** it still renews.
3. **Given** a refresh credential that never belonged to any session (random, malformed, empty), **When** it is presented, **Then** it is refused and no session is revoked.
4. **Given** two renewal requests presenting the same current credential at overlapping times, **When** both are handled, **Then** exactly one renews access and the other is refused, and the session is not revoked and its replacement credential keeps working.
5. **Given** that same pair of requests has finished, **When** the old credential is presented again afterwards, **Then** it is reuse and the session is revoked.

---

### User Story 3 - The Session Owner Can See What Happened (Priority: P2)

When a session is revoked because a replaced credential came back, the owner should not be left guessing why the device was signed out, and operators should be able to tell reuse revocations apart from ordinary sign-outs. The event is recorded for operators, and the session's revocation remains visible in the owner's session list as it is for any revoked session.

**Why this priority**: Detection that nobody can observe cannot be tuned or investigated. It is useful but the protection works without it.

**Independent Test**: Trigger a reuse revocation, then verify an operator-visible record exists that identifies the session and account and states the reason, contains no credential, and that ordinary sign-outs do not produce that record.

**Acceptance Scenarios**:

1. **Given** a session revoked by reuse, **When** an operator reviews security events, **Then** they can find an entry for that session and account stating the reason, with no credential value in it.
2. **Given** an ordinary sign-out, **When** an operator reviews security events, **Then** no reuse entry exists for it.
3. **Given** a session revoked by reuse, **When** its owner lists sessions, **Then** the session appears as revoked, like any other revoked session.

---

### Edge Cases

- **Reuse of the immediately previous credential versus an older one**: both are treated as reuse; the earlier the credential, the same outcome.
- **Two renewals at the same instant with the same credential** (for example, two browser tabs refreshing together): at most one succeeds. The other is refused and issues no credential, but the session is not revoked, because the two requests overlapped in time. A request presenting that same credential after the winning renewal has completed is reuse and follows the normal rule.
- **A legitimate client loses the response to a successful renewal** (network failure after the service replaced the credential) and retries with the old credential: this is indistinguishable from reuse and ends the session. The person signs in again. This is an accepted cost of the feature (see Assumptions).
- **Reuse of a credential whose session has already expired or been revoked**: no change; refused as today, and no new reuse event is recorded for a session that was already ended.
- **Reuse after the retention period**: once the service has forgotten a session entirely, an old credential is simply unknown and is refused without any session being affected.
- **The account is suspended or deleted before the replay**: refused as today; all its sessions are already revoked.
- **The store holding session records is unavailable during a reuse attempt**: the request fails as renewal does today and issues no credential; it must not be treated as a successful renewal.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: When a refresh credential that was previously replaced by a renewal is presented for renewal, the system MUST refuse the request, issue no credential, and revoke the session that credential belonged to.
- **FR-002**: Revoking a session for reuse MUST have the same effect as signing that session out: its current refresh credential stops working, and access credentials issued for it stop being accepted on protected endpoints immediately, for as long as they could otherwise remain valid.
- **FR-003**: Reuse of any earlier credential of a session, not only the most recent one, MUST be detected, for as long as the session's records are retained.
- **FR-004**: Reuse of a credential MUST affect only the session it belonged to. Other sessions of the same account and sessions of other accounts MUST NOT change.
- **FR-005**: The refusal returned for a reused credential MUST be indistinguishable to the caller from the refusal for any other unusable refresh credential, so the response does not tell an attacker whether a captured credential was ever valid.
- **FR-006**: Renewing with the current, unreplaced credential MUST behave exactly as it does today: access is renewed, the credential is replaced, the session receives a full new lifetime, and the session is not revoked.
- **FR-007**: A credential that never belonged to any session, or is empty or malformed, MUST be refused without revoking any session.
- **FR-008**: Reuse of a credential belonging to a session that is already revoked or expired MUST be refused without changing that session and without recording a new reuse event.
- **FR-009**: The system MUST NOT keep or log any refresh credential in readable form in order to detect reuse; detection MUST work from the same non-readable form used for current credentials, and MUST NOT make session listings or other views reveal any credential, current or earlier.
- **FR-010**: Detecting reuse and ending the session MUST be a single all-or-nothing step: a failure part-way MUST NOT leave a session that was reported as revoked but still renews or still has working access credentials, and it MUST NOT allow a renewal to succeed after reuse has been detected.
- **FR-011**: Each reuse revocation MUST produce an operator-visible record identifying the session and account and stating that the reason was credential reuse. The record MUST NOT contain any credential value. Ordinary sign-outs MUST NOT produce this record.
- **FR-012**: The memory of earlier credentials MUST NOT grow without limit: it MUST last no longer than the session's own retained records, and MUST disappear with them.
- **FR-013**: When two renewal requests present the same current refresh credential at overlapping times, exactly one MUST succeed; the other MUST be refused with no credential issued and MUST NOT revoke the session. Only a presentation that begins after the successful renewal has completed counts as reuse under FR-001.
- **FR-014**: This feature MUST NOT add a grace period during which a replaced credential is still accepted, MUST NOT revoke other sessions of the account on reuse, and MUST NOT change how sessions are listed, signed out, or revoked by account changes, apart from the new reuse revocation. Reuse detection applies to renewal only: signing out with a replaced credential keeps its current behavior.

### Key Entities

- **Authentication Session**: One sign-in for one account and device. Gains one new way to end: revocation caused by reuse of a replaced refresh credential. Its fields and lifetime rules otherwise do not change.
- **Refresh Credential**: The secret that proves a person may renew a session. Each session has one current credential; each renewal replaces it. Earlier credentials are remembered only in non-readable form and only so that their reuse can be recognised.
- **Reuse Event**: An operator-visible record that a replaced credential was presented, naming the session and account, with no credential value.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In acceptance tests, 100% of attempts to present a previously replaced refresh credential (immediately previous or any earlier one) are refused, revoke exactly that session, and result in zero further credentials issued for it.
- **SC-002**: After a reuse revocation, 100% of access credentials issued for that session are rejected on their next use, with no waiting period.
- **SC-003**: For every account with several sessions, a reuse revocation leaves 100% of the other sessions usable.
- **SC-004**: Across a test series of at least 50 consecutive in-order renewals on one session, zero are refused and zero revoke the session.
- **SC-005**: For 100% of reuse attempts, the caller-visible response is identical to the response for an unknown refresh credential.
- **SC-006**: 100% of reuse revocations produce exactly one operator-visible record, and 0% of those records, test fixtures, or session views contain a credential value.
- **SC-007**: After the session's retention period, the memory of its earlier credentials is fully gone (0 left over).

## Assumptions

- **Refresh credentials are already replaced on every renewal** (specs 004 and 007). This feature adds detection on top of that and does not change how renewal works. Earlier specs listed replay detection as out of scope; this feature is the follow-up that adds it, and it supersedes those exclusions.
- **Revoke only the affected session**, not every session of the account. A reused credential tells us about one session; revoking the others would sign out unrelated devices on a suspicion we cannot attribute to them. Account-wide sign-out remains available through the existing "sign out other devices" and password-change paths.
- **No grace period**: a replaced credential is never accepted again, even once. This means a legitimate client that loses the response to a successful renewal and retries will be signed out and must sign in again. This is accepted as the cost of an unambiguous rule; clients should be built not to retry a renewal with an old credential. A grace window can be considered later if this proves to be a support burden.
- **Earlier credentials are remembered for the session's retention period**, the same period during which expired and revoked sessions are already kept readable. This bounds storage and means reuse is only detected while the session record exists.
- **The existing session store keeps its records until they expire.** Detection and immediate access-credential revocation both rely on session records and revocation entries not being discarded early; making that guarantee in the store's configuration is a separate hardening task and is not part of this feature.
- **Existing sessions are not retrofitted.** A session already replaced before this feature is deployed has no remembered earlier credentials, so reuse of those credentials is refused as unknown rather than detected. Detection applies to replacements made after deployment.
- **Out of scope, to be handled separately**: restructuring how revoked access credentials are checked on each request, removing duplicated session rules between the domain model and the store scripts, changing store persistence or eviction settings, and alerts or notifications sent to the account owner (the operator-visible record is the only signal here).
- **Dependencies**: relies on the existing session lifecycle, the existing immediate access-credential revocation on sign-out, and the existing operator-facing logging; it adds no new external service.
