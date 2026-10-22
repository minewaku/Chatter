# Feature Specification: Abuse Throttling and Outage Resilience

**Feature Branch**: `identity-access-next`

**Created**: 2026-10-19

**Status**: Draft

**Input**: User description: "Read this conversation as input" — a discussion of how `identity-access-next` should protect itself against abuse and against failures of the things it depends on. It concluded that the service should add (1) throttling on the abuse-prone actions, keyed by the account or address being targeted rather than only by the caller, (2) graceful behavior when the mail provider is failing, (3) fast, safe failure when the session store is slow or down, and (4) visibility of that health. The shared front door already applies a coarse per-caller request limit; this feature covers what that front door cannot see.

## Clarifications

### Session 2026-10-19

- Q: Is this redundant with the rate limiting that the front door (API gateway) already does? → A: No. The front door limits how many requests a caller sends. It cannot see which account is being targeted or whether an attempt failed. This feature limits the account-level actions the front door cannot judge, and it is stricter and meaning-based where the front door is generous and count-based.
- Q: Should repeated failed sign-ins lock the account? → A: No. The account is throttled for a short, expiring period, not locked, so that an attacker cannot lock a victim out on purpose. Attempts made by the real owner after the period ends work normally.
- Q: What happens to the two sibling concerns found in the front door (a shared bucket across route groups, and a caller address the client can forge)? → A: They are out of scope here. They belong to the front-door service and are noted as a dependency.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Repeated Failed Sign-Ins Are Throttled Per Account (Priority: P1)

Someone keeps guessing the password of one account, possibly from many different network addresses so a per-caller limit never triggers. After a small number of failed sign-ins for the same account within a short window, further sign-in attempts for that account are refused for a cooling-off period, whatever address they come from. A successful sign-in by the owner clears the count. The refusal says to try again later and when, and never reveals whether the account exists.

**Why this priority**: Password guessing is the most common attack on an identity service, and a per-caller limit cannot stop an attacker who spreads attempts across addresses. This is the main reason to have the limit inside the service.

**Independent Test**: Make the allowed number of failed sign-in attempts against one account from different addresses, then make one more (even with the correct password). Confirm it is refused as "too many attempts" with a retry time, that the same happens for an email that has no account, and that after the cooling-off period a correct sign-in works.

**Acceptance Scenarios**:

1. **Given** an active account, **When** the allowed number of failed sign-ins for that account occurs within the window, **Then** the next sign-in attempt for that account is refused as "too many attempts" and states how long to wait, even if the password is correct.
2. **Given** the same situation, **When** the failures came from many different caller addresses, **Then** the refusal still applies, because the limit follows the account and not the caller.
3. **Given** an email or username that has no account, **When** the same number of attempts is made, **Then** the response is identical in shape and timing class to the one for a real account, so the existence of the account is not revealed.
4. **Given** an account with some failed attempts below the limit, **When** the owner signs in successfully, **Then** the failure count for that account is cleared.
5. **Given** a throttled account, **When** the cooling-off period ends, **Then** the owner can sign in normally and the account was never locked or changed.

---

### User Story 2 - Password Confirmation Cannot Be Brute-Forced With A Stolen Credential (Priority: P1)

Several actions ask a signed-in person to confirm their current password: changing the password, asking to change the email, and closing the account. Someone holding a stolen but still-valid access credential could try to guess the password through those actions. After a small number of wrong confirmations for the same person within a window, further confirmations are refused for a cooling-off period. A wrong confirmation still does not sign the person out, as before.

**Why this priority**: These are the highest-impact actions (account takeover and destruction). Without this limit, the password confirmation turns the signed-in endpoints into a guessing oracle.

**Independent Test**: Sign in, send wrong passwords to one of these actions up to the allowed number, then send the correct one. Confirm it is refused as "too many attempts", that the other confirmed actions are refused too (the limit follows the person), and that it works after the cooling-off period.

**Acceptance Scenarios**:

1. **Given** a signed-in person, **When** they send the allowed number of wrong current passwords to any password-confirmed action, **Then** the next confirmation attempt on any of those actions is refused as "too many attempts" with a wait time, even with the correct password.
2. **Given** a refused confirmation, **When** the person looks at their session, **Then** it is still valid; being throttled never ends a session.
3. **Given** a correct password confirmation, **When** the action succeeds, **Then** the wrong-confirmation count for that person is cleared.
4. **Given** the cooling-off period has passed, **When** the person confirms with the correct password, **Then** the action proceeds normally.

---

### User Story 3 - Verification And Email-Change Mail Cannot Be Used To Flood An Inbox Or Exhaust Mail Quota (Priority: P2)

Resending a verification email, and asking for an email change, each cause an email to be sent to an address. Someone could use them to flood a victim's inbox or to burn the service's daily mail allowance. Resends to the same address are limited to a minimum gap between sends and a maximum per hour, and email-change requests are limited per person per hour. When a limit is reached the request is refused as "too many attempts" with a wait time, and no mail is sent.

**Why this priority**: It protects third parties (the inbox owner) and a finite shared resource (the mail allowance). It is less urgent than account-takeover protection but is easy to abuse.

**Independent Test**: Ask for a resend twice in a row, then repeat until the hourly cap, and confirm the second request within the minimum gap and every request over the cap are refused with no extra mail sent, while a request after the wait is accepted.

**Acceptance Scenarios**:

1. **Given** a pending account, **When** a resend is requested again within the minimum gap, **Then** it is refused as "too many attempts" with the remaining wait, and no mail is sent.
2. **Given** an address that has reached the hourly cap, **When** another resend is requested, **Then** it is refused and no mail is sent, whoever asks.
3. **Given** an address that has no account, **When** resends are requested, **Then** the visible outcome is the same as for a real address, so account existence is not revealed.
4. **Given** a signed-in person who has reached the hourly cap for email-change requests, **When** they ask for another, **Then** it is refused as "too many attempts" and nothing is sent to the new address.

---

### User Story 4 - A Failing Mail Provider Does Not Break Sign-Up And Does Not Pile Up Waiting Requests (Priority: P2)

When the mail provider is down, slow, or rejecting mail, each attempt to send currently makes the person wait through several retries. After repeated delivery failures the service should stop trying for a short time, answer quickly, and try again automatically later. Registration and email-change requests that depend on sending mail still record what the person asked for, and tell them the mail could not be sent right now so they can ask for a resend. When the provider recovers, normal sending resumes without anyone restarting anything.

**Why this priority**: Mail is the dependency most likely to fail, and it sits on the critical path of registration. Waiting through retries against a dead provider ties up capacity and makes the whole service feel slow.

**Independent Test**: Simulate the mail provider failing repeatedly, then register. Confirm the answer returns quickly with the "mail could not be sent, try resending" outcome, the account exists in the pending state, and that after the provider recovers a resend succeeds without a restart.

**Acceptance Scenarios**:

1. **Given** repeated mail delivery failures, **When** a new person registers, **Then** the response comes back within the normal response time, the account is created pending verification, and the response tells them verification mail is delayed and they may request a resend.
2. **Given** the service has stopped trying to send because of failures, **When** a short period passes and the provider has recovered, **Then** the next send is attempted and succeeds without manual action.
3. **Given** an email-change request while sending is stopped, **When** the person submits it, **Then** they are told the confirmation could not be sent and the account's current email is unchanged.
4. **Given** any delivery failure, **When** it is logged, **Then** no token, link, or password appears in the log or in the response.

---

### User Story 5 - A Slow Or Down Session Store Fails Fast And Safely (Priority: P3)

Every signed-in request checks whether its session was revoked. If the store that holds that information is slow or unreachable, each request currently waits for a full timeout before failing. Instead, the wait must be short and bounded, and after repeated failures the service should answer "temporarily unavailable" immediately for a short time and then try again automatically. It must never fail open: a request whose revocation status cannot be determined is refused, never allowed.

**Why this priority**: A store outage is rare but severe. Failing fast keeps the service responsive and protects against a pile-up, and failing closed keeps security intact. It is lower priority because the existing behavior is already safe, just slow.

**Independent Test**: Make the session store unreachable and send signed-in requests. Confirm each is refused with "temporarily unavailable" quickly, that none succeed, and that once the store is back, requests succeed again without a restart.

**Acceptance Scenarios**:

1. **Given** an unreachable session store, **When** a signed-in request arrives, **Then** it is refused as "temporarily unavailable" within a short bounded time and is never allowed through.
2. **Given** repeated store failures, **When** further requests arrive, **Then** they are refused immediately without waiting on the store, until a short period passes.
3. **Given** the store has recovered, **When** the next request arrives, **Then** normal checking resumes without manual action.
4. **Given** a request that carries a revoked session, **When** the store is healthy, **Then** it is still refused as "unauthenticated", unchanged.

---

### User Story 6 - Operators Can See Dependency Health (Priority: P3)

The person running the service can see, from the existing health information, whether sending mail and the session store are currently healthy or being held off because of failures, so an outage is visible without reading logs.

**Why this priority**: Protective behavior that cannot be observed is hard to trust or debug. It is small and builds on the stories above.

**Independent Test**: Cause each dependency to fail until the service holds off, and confirm the health information reports it as degraded; confirm it reports healthy again after recovery.

**Acceptance Scenarios**:

1. **Given** the service is holding off sending mail, **When** an operator reads the health information, **Then** it shows mail as degraded without exposing credentials or recipients.
2. **Given** the service is holding off on the session store, **When** an operator reads the health information, **Then** it shows the store as degraded.
3. **Given** both dependencies are healthy, **When** an operator reads it, **Then** both show healthy.

---

### Edge Cases

- What happens when the failure counters themselves cannot be stored (the same store is down)? Sign-in and the password-confirmed actions must not become unlimited by accident: they fail closed with "temporarily unavailable", consistent with how revocation is handled.
- What happens when the same person is throttled on sign-in while already signed in on another device? The signed-in session is unaffected; only new sign-in attempts for that account are held.
- What happens when an attacker deliberately fails attempts for a victim to keep them throttled? The throttle is short and expires on its own, the account is never locked, and a successful owner sign-in after expiry works; a repeated denial of service of this kind is accepted as a known trade-off.
- What happens when two requests race at the limit boundary? At most the allowed number succeed or are counted; the limit is never exceeded by concurrent requests.
- What happens when the clock or the wait time is read by the client? The wait time returned is the actual remaining time and is never longer than the cooling-off period.
- What happens when a throttled request has an invalid body? Validation failures are reported as before and do not count as failed attempts.
- What happens to the front door's own limit? Both limits apply to a request, the stricter one wins, and clients receive the same "too many attempts" shape from either.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST count failed sign-in attempts per targeted account identifier (email or username, normalized) within a time window, independent of the caller's network address.
- **FR-002**: Once the allowed number of failed sign-ins for an account identifier is reached, the system MUST refuse further sign-in attempts for it, including ones with the correct password, until the cooling-off period ends.
- **FR-003**: The refusal MUST be a distinct "too many attempts" outcome that states the remaining wait time, and MUST be identical in form for identifiers that have no account.
- **FR-004**: A successful sign-in MUST clear that account identifier's failed-attempt count.
- **FR-005**: The system MUST NOT lock or alter an account because of throttling; throttling MUST expire on its own.
- **FR-006**: The system MUST count wrong current-password confirmations per signed-in person across all password-confirmed actions (password change, email-change request, account closure), and MUST refuse further confirmations as "too many attempts" once the allowed number is reached, until the cooling-off period ends.
- **FR-007**: A wrong or throttled confirmation MUST NOT end the person's session, and a correct confirmation MUST clear the count.
- **FR-008**: The system MUST limit verification resends per target address by a minimum gap between sends and a maximum number per hour, and MUST send no mail when refused.
- **FR-009**: The system MUST limit email-change requests per signed-in person per hour, and MUST send no mail when refused.
- **FR-010**: Resend refusals MUST NOT reveal whether the address has an account.
- **FR-011**: Failed validation (malformed or invalid input) MUST NOT count toward any attempt limit.
- **FR-012**: The system MUST apply every limit atomically so concurrent requests cannot exceed it.
- **FR-013**: The "too many attempts" outcome MUST use the same error shape as other refusals, be documented on every endpoint that can return it, and include a standard retry-after indication.
- **FR-014**: If the state needed to enforce a limit cannot be read or written, the system MUST fail closed with "temporarily unavailable" and MUST NOT treat the request as unlimited.
- **FR-015**: When sending mail has failed repeatedly, the system MUST stop attempting sends for a short period, answer quickly, and resume automatically after that period.
- **FR-016**: Registration MUST still create the pending account when verification mail cannot be sent, and the response MUST tell the person that mail is delayed and a resend may be requested.
- **FR-017**: An email-change request MUST leave the current email unchanged and MUST tell the person the confirmation could not be sent when sending is unavailable.
- **FR-018**: Mail delivery failures MUST NOT expose tokens, links, passwords, or recipient lists in logs or responses.
- **FR-019**: The session revocation check MUST have a short bounded wait, MUST stop querying the store for a short period after repeated failures, and MUST resume automatically.
- **FR-020**: When revocation status cannot be determined, the request MUST be refused as "temporarily unavailable" and MUST NEVER be allowed through.
- **FR-021**: The existing health information MUST report the mail path and the session store as degraded while the service is holding off on them, and healthy otherwise, without exposing credentials or recipients.
- **FR-022**: All limits, windows, cooling-off periods, minimum gaps, and failure thresholds MUST be adjustable by configuration without a code change, with documented safe defaults.
- **FR-023**: Existing behavior for every request below its limits MUST be unchanged.

### Key Entities

- **Attempt Counter**: A short-lived record of how many failed attempts occurred for one subject (an account identifier, a person, an address) within a window, with the time it expires.
- **Throttle Subject**: What a limit follows. For sign-in it is the account identifier, for password confirmation it is the signed-in person, for resends it is the target address, for email-change requests it is the signed-in person.
- **Throttle Policy**: The configured allowed count, window, cooling-off period, and minimum gap for one kind of action.
- **Dependency Health**: Whether the mail path or the session store is currently usable or being held off, and when the next attempt will be made.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After the allowed number of failed sign-ins for one account, 100% of further sign-in attempts for it, from any number of different callers, are refused for the cooling-off period.
- **SC-002**: With wrong current passwords sent to the password-confirmed actions, a signed-in person cannot make more than the allowed number of guesses per window across all those actions combined.
- **SC-003**: The refusal for a non-existent account is indistinguishable from the refusal for an existing one in a blind comparison of responses.
- **SC-004**: A single address cannot be sent more than the allowed number of verification mails per hour, and no mail is sent for refused requests.
- **SC-005**: While the mail provider is failing, 95% of registration requests still return within 2 seconds and 100% of them leave a pending account behind.
- **SC-006**: While the session store is unreachable, 100% of signed-in requests are refused (none allowed through), and 95% of those refusals return within 2 seconds.
- **SC-007**: After a dependency recovers, normal behavior resumes within the configured hold-off period with no manual restart.
- **SC-008**: An operator can tell from the health information within one check whether mail or the session store is degraded.
- **SC-009**: No legitimate flow below its limits changes behavior: the existing automated test suite still passes unchanged.

## Assumptions

- The front door (API gateway) keeps applying its coarse per-caller request limit; this feature does not replace it, and it is allowed to also reject a request before it reaches the service.
- The two issues found in the front door (route groups sharing one bucket, and a caller address the client can forge) are fixed in that service separately and are outside this feature.
- The service runs as more than one instance in the future, so attempt counts are shared state rather than per-instance memory.
- Failed-attempt state may be lost if the shared store is wiped; the effect is only that a limit starts over, which is acceptable for a personal project.
- Initial defaults (adjustable): 5 failed sign-ins per account per 15 minutes; 5 wrong password confirmations per person per 15 minutes; resend minimum gap 1 minute and 5 per hour per address; 3 email-change requests per person per hour.
- Registration is not given its own per-address throttle in this feature; the front door limit is considered enough for now.
- Account lockout, CAPTCHA, multi-factor challenges, and IP reputation or blocklists are out of scope.
- Retry of transient failures already exists for mail delivery and is kept; this feature adds the hold-off after repeated failures.
- A time-limit or concurrency cap for slow calls and a hold-off around the relational database are out of scope; database waits are bounded by the existing connection settings.
- The existing error shape and codes are reused, with one new "too many attempts" outcome added.
