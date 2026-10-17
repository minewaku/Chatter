# Feature Specification: Identity Access Presentation API

**Feature Branch**: `master`

**Created**: 2026-10-17

**Status**: Draft

**Input**: User description: "Implement the `identityaccess-presentation` module of `identity-access-next`: the HTTP API layer exposing the core use cases from specs 001 and 002 plus the existing register and soft-delete use cases. Use the old `identity-access` presentation module as a structural reference, but do not copy its defects. Sign-in returns both credentials as a JSON body. Status codes, error format, paths, and shapes are delegated to the author."

## Clarifications

### Session 2026-10-17

- Q: Should a session have a hard maximum lifetime, even if the person keeps using it? → A: No cap. Each renewal starts a new full session lifetime (30 days by default), and a session renewed before each expiry lasts indefinitely until revoked. The term for this behavior is "renewal", not "expansion".

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Register, Verify, and Sign In Over HTTP (Priority: P1)

A new person registers with email, username, birthday, and password, follows the link in the verification email, and then signs in. Sign-in answers with an access credential and a refresh credential in a JSON body. An already-registered person is told so, and a registered-but-unverified person can ask for a new verification email.

**Why this priority**: Without these endpoints no client can create or use an account. Every other story depends on a signed-in caller.

**Independent Test**: Against a running service, register, confirm the verification token, sign in, and call a protected endpoint with the returned access credential. Then repeat each step with bad input and confirm the documented error outcome.

**Acceptance Scenarios**:

1. **Given** a new email and username, **When** the caller registers, **Then** the response reports creation, contains no password, hash, or token, and a verification email is requested.
2. **Given** an email or username already in use by an active account, **When** the caller registers, **Then** the response is a conflict with the stable code `account_already_exists` and nothing new is stored.
3. **Given** a registered account still awaiting verification, **When** the caller registers again with the same details, **Then** the response is a conflict with the stable code `verification_pending`, and no second verification email is sent.
4. **Given** a pending account, **When** the caller requests a verification email for its address, **Then** the response is the same "accepted" answer whether or not the address belongs to an eligible account.
5. **Given** a valid unexpired verification token, **When** the caller confirms it, **Then** the account becomes active and a repeat of the same confirmation reports it as already verified without error.
6. **Given** a blank, unknown, invalidated, or expired token, **When** the caller confirms it, **Then** the response is a client error with the stable code `invalid_or_expired_token`.
7. **Given** an active account and correct password, **When** the caller signs in, **Then** the response body is `{"accessToken": "...", "refreshToken": "..."}`, and no credential is set as a cookie or header.
8. **Given** an unknown email, a wrong password, or an account that is not active, **When** the caller signs in, **Then** the response is one identical "unauthenticated" outcome that reveals nothing about which condition applied.

---

### User Story 2 - Keep a Session Alive and End It (Priority: P1)

A signed-in client exchanges its refresh credential for a new access credential and a new refresh credential when the old access credential expires. Each renewal gives the session a new full lifetime (the configured session period, 30 days by default), so a session that keeps being used stays signed in indefinitely. When the person signs out, the client presents the refresh credential, the session ends, and access credentials already issued for that session stop working right away instead of lingering until they expire.

**Why this priority**: Access credentials are short-lived. Without renewal, people are forced to sign in again constantly, and without immediate sign-out a stolen or abandoned access credential stays usable.

**Independent Test**: Sign in, renew, sign out, then confirm the old access credential is refused on a protected endpoint and a further renewal with the last refresh credential is refused.

**Acceptance Scenarios**:

1. **Given** a usable session, **When** the client presents its refresh credential, **Then** the response is `{"accessToken": "...", "refreshToken": "..."}` with a new refresh credential, the session's expiry moves forward, and its last-used time advances.
2. **Given** a refresh credential that has already been exchanged for a newer one, **When** the client presents it again, **Then** the response is the generic "unauthenticated" outcome and the session is unaffected.
3. **Given** a blank, malformed, unknown, expired, or revoked refresh credential, **When** the client tries to renew, **Then** the response is the generic "unauthenticated" outcome.
4. **Given** a usable session, **When** the client signs out with its refresh credential, **Then** the session is revoked, later renewal fails, and the session's access credentials are refused on protected endpoints immediately.
5. **Given** the same person is signed in on two devices, **When** one device signs out, **Then** the other device's access and refresh credentials keep working.
6. **Given** an unknown, expired, or already revoked refresh credential, **When** the client signs out, **Then** the response is the same success outcome as a first sign-out. The caller cannot tell whether a session existed.
7. **Given** either endpoint is called without an access credential, or with an expired or invalid one, **When** the request is made, **Then** it proceeds, because both are public.

---

### User Story 3 - See and Manage My Sessions (Priority: P2)

A signed-in person lists the sessions on their account (device label, issued, expires, last used, revoked), ends one specific session, or ends every session except the one they are using. Both destructive actions require the current password.

**Why this priority**: This is the main account-safety tool. It is not needed to sign in or stay signed in, so it ranks below Stories 1 and 2.

**Independent Test**: Sign in on two devices, list sessions from one, end the other by identifier with the password, confirm the other can no longer renew, then sign in again and end all others.

**Acceptance Scenarios**:

1. **Given** a signed-in caller, **When** they list sessions, **Then** they receive only their own sessions, ordered newest first, with no refresh credential or digest.
2. **Given** a caller's own retained session and the correct password, **When** they end that session, **Then** it is revoked and the response confirms it.
3. **Given** a session owned by someone else, or one that does not exist, **When** the caller tries to end it, **Then** the response is "not found" with no hint about who owns it.
4. **Given** a wrong password, **When** the caller tries to end one or all other sessions, **Then** the request is refused, no session changes, and the response is distinct from "unauthenticated" so clients do not mistake it for an expired access credential.
5. **Given** the caller's current usable session and the correct password, **When** they end all other sessions, **Then** every other session is revoked, the current one (identified by the caller's access credential, not by anything the client sends) still works, and the response reports how many were ended. Repeating it reports zero. Access credentials of the ended sessions are refused immediately.
6. **Given** no access credential or an invalid one, **When** any of these endpoints is called, **Then** the response is "unauthenticated" and no logic runs.

---

### User Story 4 - Manage My Own Account (Priority: P2)

A signed-in person changes their password or closes their account. Changing the password ends all of their sessions, including the one in use, so they sign in again. Closing the account makes it inaccessible and also ends all of its sessions.

**Why this priority**: These are existing core capabilities that need an HTTP surface, but they are not part of first use.

**Independent Test**: Change the password with the correct current password and confirm the new one signs in and the old one does not. Close the account and confirm sign-in is refused.

**Acceptance Scenarios**:

1. **Given** a signed-in caller who supplies the correct current password and an acceptable new one, **When** they change the password, **Then** the change is confirmed and the response contains neither password.
2. **Given** a wrong current password, **When** they change it, **Then** the request is refused and nothing changes.
3. **Given** a new password that violates password rules, **When** the caller submits it, **Then** the response is a validation error that names the field and not the value.
4. **Given** a signed-in caller, **When** they close their own account, **Then** the account is soft-deleted and the response is success with no body. The target account is always the one named by the access credential.

---

### User Story 5 - Consistent Errors and Safe Logging (Priority: P1)

Every failure from every endpoint has the same JSON shape with a stable machine-readable code. No endpoint, log line, or error ever contains a password, verification token, or refresh credential.

**Why this priority**: Clients cannot be built reliably against inconsistent errors, and the old service logged raw passwords.

**Independent Test**: Trigger each documented failure and compare response shapes. Then run the full flow with sentinel values for password, token, and refresh credential, and search all logs and responses for those sentinels.

**Acceptance Scenarios**:

1. **Given** a malformed JSON body, a missing required field, or a field with an invalid format, **When** a request is made, **Then** the response is a client error with the standard error shape and a list of the offending field names.
2. **Given** an unexpected internal failure, **When** a request is made, **Then** the response is a server error with the standard shape, a generic message, and no stack trace or internal detail.
3. **Given** any request, **When** it is processed, **Then** no log entry or response contains the submitted password, new password, verification token, or refresh credential.

---

### Edge Cases

- A request arrives with an access credential whose subject is not a valid account identifier: treat it as unauthenticated rather than failing with a server error.
- The caller's account has become inaccessible (suspended or deleted) after the access credential was issued: session and account endpoints return the core outcome mapped as refused, not a server error.
- A sign-in request has no `User-Agent` header, or one longer than the device-label limit: the device label falls back to a default or is truncated so sign-in still succeeds.
- A verification link is opened twice: the second visit reports "already verified" rather than failing.
- A request body is empty, malformed, has the wrong content type, or has a field longer than its limit: a client error in the standard shape.
- Ending the other sessions when the session named by the caller's access credential is no longer usable: refused, with no session changed.
- Two renewals race with the same refresh credential (for example two browser tabs): the first wins and the second gets the generic "unauthenticated" outcome. Clients are expected to serialize renewals.
- An access credential issued before this feature (no session claim): refused as unauthenticated, so the caller signs in again.
- The revocation store is unreachable when a protected request arrives: the request is refused with a service-unavailable error rather than allowed through.
- The revocation store loses its entries (restart or eviction): revoked access credentials work again only until their own expiry, never longer. The session record stays revoked, so renewal still fails.
- Verification confirmation is reached by following an emailed link in a browser: the response must be understandable there, not only to a script.
- Browser preflight requests succeed without a credential, with allowed origins taken from configuration.
- A client sends an expired or invalid access credential to a public endpoint (typically renewal, because that is when the access credential has just expired): the credential is ignored and the public endpoint proceeds.

## Requirements *(mandatory)*

### Functional Requirements

**Endpoints**

- **FR-001**: The API MUST expose these public endpoints, with no access credential required:
  - register: `POST /api/v1/auth/register`
  - sign in: `POST /api/v1/auth/authenticate`
  - request a verification email: `POST /api/v1/auth/verification/resend`
  - confirm a verification token: `GET /api/v1/auth/verification/confirm`
  - renew access: `POST /api/v1/auth/refresh`
  - sign out the presented session: `POST /api/v1/auth/logout`
- **FR-002**: The API MUST expose these protected endpoints, which require a valid access credential:
  - list own sessions: `GET /api/v1/sessions`
  - end one session: `POST /api/v1/sessions/{sessionId}/revoke`
  - end all other sessions: `POST /api/v1/sessions/revoke-others`
  - change own password: `PUT /api/v1/users/@me/password`
  - close own account: `DELETE /api/v1/users/@me`
- **FR-003**: The public set in FR-001, plus API documentation, MUST be exactly the allow-list that the security configuration from spec 003 FR-012 permits. Any other path MUST require a valid access credential.
- **FR-004**: The old endpoints that were unimplemented stubs (end one session, end other sessions) MUST be real in this feature, and the old cookie-based credential transport MUST NOT be carried over.

**Request and response shapes**

- **FR-005**: All request and response bodies MUST be JSON, including fields that the old service took as a bare plain-text string such as `password`.
- **FR-006**: Registration MUST accept email, username, birthday, and password. It MUST respond `201` with a body containing only the new account identifier on success, and `409` on `AccountAlreadyExists` (code `account_already_exists`) or `VerificationPending` (code `verification_pending`).
- **FR-007**: Sign-in MUST accept email and password, derive a device label from the request, and respond `200` with exactly `{"accessToken": "...", "refreshToken": "..."}`. Credentials MUST NOT be set as cookies or exposed in headers or URLs.
- **FR-008**: Renewal MUST accept `{"refreshToken": "..."}` in the request body and respond `200` with `{"accessToken": "...", "refreshToken": "..."}`. The presented refresh credential MUST stop working once the new one is issued, and the session's expiry MUST move forward (see FR-029).
- **FR-009**: Sign-out MUST accept `{"refreshToken": "..."}` and respond `204` with no body whether or not a session was revoked. When a session was revoked, FR-030 applies.
- **FR-010**: A verification-email request MUST accept `{"email": "..."}` and respond `202` with no body, regardless of whether the address matches an eligible account.
- **FR-011**: Verification confirmation MUST accept the token as the `token` query parameter. It MUST respond `200` with a small JSON body whose `status` is `verified` or `already_verified`, and `400` with code `invalid_or_expired_token` otherwise.
- **FR-012**: Session listing MUST respond `200` with a JSON array of session views containing session identifier, device label, issued time, expiry, last used time, and revoked time, ordered as the core query orders them. It MUST NOT include a refresh credential, digest, or account identifier of another user.
- **FR-013**: Ending one session MUST accept `{"currentPassword": "..."}`. It MUST respond `204` when newly revoked, and `404` with code `session_not_found` when the core reports not revoked, with no information on ownership.
- **FR-014**: Ending all other sessions MUST accept `{"currentPassword": "..."}`. The current session MUST be the one named by the caller's access credential, never a client-supplied identifier. It MUST respond `200` with `{"revokedCount": n}`.
- **FR-015**: Changing a password MUST accept `{"currentPassword": "...", "newPassword": "..."}` and respond `204` when changed, or `409` with code `password_unchanged` when the core reports no change.
- **FR-016**: Closing an account MUST take its target only from the authenticated credential's subject and respond `204`.

**Errors**

- **FR-017**: Every non-success response MUST use one JSON body containing at least `status` (number), `code` (stable lowercase identifier), `message` (human-readable and generic), and `timestamp`. Validation failures MUST add `errors`, a list of `{field, message}` with no submitted values.
- **FR-018**: The API MUST map outcomes consistently:
  - `400` malformed input or validation failure
  - `401` missing, invalid, or expired access credential, and any failed sign-in or renewal, using one generic message
  - `403` authenticated but refused, such as wrong current password or inaccessible account
  - `404` session not found
  - `409` conflict
  - `500` unexpected failure
  - `503` the revocation state cannot be read (FR-033)
- **FR-019**: A wrong current password MUST NOT produce `401`, so clients do not confuse it with an expired access credential.
- **FR-020**: Unexpected errors MUST NOT expose stack traces, class names, SQL, or other internal detail. The catch-all handler that turns any illegal-argument or illegal-state error into a `400` with the raw message MUST be replaced by explicit mappings.

**Security and privacy**

- **FR-021**: Protected endpoints MUST take the account identity only from the validated access credential. No client-supplied user identifier is accepted for account or session operations.
- **FR-022**: No log entry, response, or error may contain a submitted password, new password, verification token, or refresh credential. Request and response body logging MUST NOT be present.
- **FR-023**: Sign-in, renewal, and verification-request responses MUST NOT reveal whether an email, account, or session exists, apart from the registration conflicts in FR-006.
- **FR-024**: Cross-origin permissions and the public base address used in verification links MUST come from configuration, as in spec 003.
- **FR-025**: Signing keys under `presentation/user/.local/` MUST NOT be committed or relied on by this module. Keys are loaded through the configuration defined in spec 003.

**Refresh rotation and access-credential revocation**

- **FR-029**: Renewal MUST rotate the refresh credential: the session keeps its identifier, receives a new opaque refresh credential and a new full lifetime measured from the renewal, and the old credential becomes unusable. There is no maximum total session age: a session that is renewed before each expiry never has to sign in again, unless it is revoked. Replay detection that revokes the whole session on reuse of an old credential is out of scope.
- **FR-030**: Every access credential MUST name the session it belongs to. When a session is revoked, access credentials of that session MUST be refused on protected endpoints from the next request until their own expiry, and access credentials of other sessions MUST be unaffected.
- **FR-031**: The same refusal MUST apply on every path that revokes sessions: sign-out, ending one session, ending other sessions (all except the caller's current one), and the account-wide revocation that follows a password change, suspension, or account closure. No path may leave a revoked session's access credentials usable.
- **FR-032**: Revocation entries MUST be kept only as long as the longest-lived access credential they could affect, and then disappear on their own. The permanent record that a session was revoked stays in the session record, not in the revocation entries.
- **FR-033**: If revocation state cannot be read, protected requests MUST fail closed with `503` and code `service_unavailable`, and MUST NOT be allowed through. Public endpoints are unaffected.
- **FR-034**: Clients MAY also simply discard their access credential on sign-out. Server-side refusal in FR-030 is in addition to that, not a replacement.

- **FR-035**: Public endpoints MUST ignore any `Authorization` header, valid or not, so a client holding an expired access credential can still renew, sign in, or sign out.

**Structure and documentation**

- **FR-026**: The presentation module MUST depend only on core's public use-case contracts and MUST NOT reference infrastructure or persistence types. A structure check MUST fail the build otherwise.
- **FR-027**: Controllers MUST be organised by business capability, and request, response, and error types MUST be separate from the core result types so that internal changes do not leak into the API.
- **FR-028**: The API MUST publish machine-readable documentation for every endpoint, including request, response, and error shapes.

### Key Entities

- **Credential pair**: The access and refresh credentials returned at sign-in and at every renewal. A refresh credential is shown only in the response that issues it.
- **Revocation entry**: A short-lived marker, one per revoked session, that makes that session's access credentials unusable until they expire. It names the account and the session.
- **Session view**: The safe projection of one retained session returned by listing.
- **Error response**: The single JSON shape for every failure, with a stable code and optional field errors.
- **Device label**: A short human-readable description of the signing-in client, derived from the request and at most 200 characters.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new person can register, verify, and sign in using only the documented endpoints, with a token pair returned, in a single scripted run.
- **SC-002**: 100% of the 11 endpoints in FR-001 and FR-002 return the success status and body shape defined for them, in automated end-to-end tests.
- **SC-003**: 100% of protected-endpoint requests with a missing, malformed, expired, tampered, or foreign-issuer access credential return `401` in the standard error shape, with no endpoint logic executed.
- **SC-004**: 100% of failure responses across the endpoint suite have the same top-level fields, and none exposes a stack trace or internal class name.
- **SC-005**: In end-to-end tests, 0 occurrences of sentinel passwords, verification tokens, or refresh credentials appear in any log output or in any response other than the sign-in and renewal responses that issue a refresh credential.
- **SC-006**: Identical responses (status and body) are returned for sign-in with an unknown email, a wrong password, and an inactive account, and for verification requests with eligible and ineligible emails.
- **SC-007**: After ending a session or all other sessions, 100% of renewal attempts with the ended sessions' credentials are refused.
- **SC-008**: After sign-out, ending a session, or an account-wide revocation, 100% of protected requests using an access credential of the ended session are refused starting with the next request, while 100% of requests using credentials of the person's other sessions still succeed.
- **SC-009**: After renewal, 100% of attempts to reuse the previous refresh credential are refused, and the session's expiry is a full session lifetime after the renewal, with no upper limit on how often this can repeat.
- **SC-010**: With the revocation store emptied, no revoked access credential remains usable past its own expiry, and 0 revoked sessions can renew.

## Assumptions

- The core use-case contracts from specs 001 and 002 and the existing register, change-password, and soft-delete use cases are the base. This feature adds no new account or verification behavior.
- Spec 003 delivers the resource-server security configuration, key loading, and the public-endpoint allow-list. This feature only supplies the endpoints and keeps the allow-list in step with FR-001.
- **Changes this feature makes to earlier specs.** (a) Spec 002: renewal now rotates the refresh credential and renews the session lifetime (FR-029), so `RefreshAccessTokenUseCase.Refreshed` gains a refresh credential and the session port gains a rotate operation. (b) Spec 002/003: the access credential carries the session identifier (spec 003 FR-005), and the ability to issue it takes the session. (c) Spec 003: the resource-server check gains a revocation lookup (FR-030 to FR-033), and spec 003 FR-006's "no replay handling" still stands. These are plan-level changes to existing modules and are listed here so they are not forgotten.
- **Revocation design and why.** Sessions already hold the permanent record of revocation, so revocation entries only need to bridge the gap until the access credential's own expiry. They are therefore short-lived entries in the existing key-value store, with a lifetime equal to the configured access-credential lifetime. They are not stored permanently: a permanent list would grow without bound, duplicate the session record, and add no protection once a credential has expired. Each entry is keyed by the account and the session identifier, so signing out on one device does not sign the person out elsewhere. Keying by account alone would do exactly that. Account-wide revocation (password change, suspension, closure) and ending other sessions simply write one entry for every session they revoke, written in the same step as the revocation so no gap exists between the two.
- **Verification email requests are public and take an email.** A pending account cannot sign in, so it cannot present an access credential. Throttling stays outside core and is not part of this feature.
- Verification confirmation uses `GET` because the emailed link is opened in a browser. The token is therefore in a URL, so request-line logging of that query parameter is suppressed.
- The device label is derived from the `User-Agent` header (browser, operating system, device class), falling back to `Unknown device`, and is truncated to 200 characters. The client network address is not part of the label.
- The `/api/v1/` path prefix and the `@me` convention follow the old service. Changing the password also ends the account's sessions (existing core behavior), and the client is expected to sign in again.
- Rate limiting, CAPTCHA, account lock or unlock endpoints, username or email change endpoints, and administrator endpoints are out of scope. The corresponding core use cases are not exposed here.
- Account and session endpoints are for the account holder only. There are no roles beyond authenticated or not.
- Browser and mobile front ends are out of scope. Clients consume the JSON API directly.
