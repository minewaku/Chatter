# Research: Account Change Endpoints

All items below were resolved from the spec and the existing code. No open `NEEDS CLARIFICATION` remains.

## 1. HTTP method and path for closing

- **Decision**: `POST /api/v1/users/@me/close` with `{"currentPassword": "..."}`, `204` on success. The old `DELETE /api/v1/users/@me` is removed (FR-008).
- **Rationale**: The password must be in the body (FR-006). `DELETE` with a body has no defined meaning and is stripped by some proxies and clients. The same shape is already used for `POST /sessions/{id}/revoke` and `POST /sessions/revoke-others`, which also carry the password. A verb path (`/close`) states the action, which is not a plain deletion because the record is kept as a closed account.
- **Alternatives considered**: `DELETE /users/@me` with a body (unreliable); password in a header (secret moves out of the body and into places that proxies tend to log; adds a second convention); keeping `DELETE` and adding a re-authentication token step (new moving part for no gain).

## 2. A wrong password must not sign the person out

- **Decision**: Reuse the existing mapping: core throws `InvalidCredentialsException`, the exception handler answers `403 reauthentication_failed`. Nothing in the close or email-change path touches sessions before the password check passes.
- **Rationale**: This already holds for password change and revoke. FR-003 only needs a test pinning it for the new actions (a follow-up call with the same access credential still works).
- **Alternatives considered**: None needed; `401` would let a client treat it as an expired credential and drop the session, which is what the user asked to avoid.

## 3. What "close" verifies

- **Decision**: A new `UserAccount.close(PlainPassword, PasswordHasher)` verifies the password only, not that the account is accessible, then delegates to the unchanged no-argument `softDelete()`. An already-closed account fails the password check because its hash is the unusable placeholder, so core would answer `403`; in practice such callers get `401` first because closing ends all sessions.
- **Rationale**: Today a suspended or locked person can close their own account. Requiring "active" would be a behavior change nobody asked for. A closed account's password hash is the unusable placeholder, so a password check can never succeed for it.
- **Alternatives considered**: Reusing `verifyAccessibleCredentials` (would block suspended or locked people from closing their account; rejected as an unrequested change).

## 4. Where the pending email change is stored

- **Decision**: Redis, in a new key family `identity-access:email-change:token:<sha256(token)>` plus a per-user pointer `identity-access:email-change:user:<userId>`. The hash holds `userId`, `email` (the new address), `createdAt`, `expiresAt`, and `confirmedAt` once used. The entry expires on its own at the token's expiry.
- **Rationale**: It mirrors how registration tokens are stored, so the existing `verification-save.lua` and `verification-invalidate-for-user.lua` work unchanged with different key prefixes. A separate key family is what keeps tokens purpose-bound (FR-026) without a purpose flag that could be forgotten. At most one pending change per user (FR-021) is exactly the user-pointer script's existing job. TTL cleanup needs no job. No Flyway migration, no new column on `user_account`.
- **Alternatives considered**: A `pending_email` column or a table in PostgreSQL (needs a migration, a cleanup job for expired rows, and a second uniqueness story); reusing the verification key family with a `purpose` field (a registration token lookup would have to remember to reject email-change records, and the invalidate script would drop the wrong kind); a signed stateless token carrying the new email (cannot be replaced or revoked on demand, which FR-021 and FR-027 require).

## 5. Token shape and lifetime

- **Decision**: Same generator (`UniqueStringIdGenerator`, UUID v7), stored as a SHA-256 digest only, 24-hour lifetime, the same as `EmailVerificationIssuanceCoordinator.DEFAULT_LIFETIME`. The new service holds its own `Duration` constant of 24 hours.
- **Rationale**: FR-026 says "same lifetime as existing verification tokens". The existing constant lives in the `emailverification.internal` package, which `user` must not reach into, so a local constant with the same value is used and a unit test asserts it equals the registration lifetime's value.
- **Alternatives considered**: A shared configuration property (nicer long term, but neither flow is configurable today; out of scope); importing the coordinator's constant (breaks package boundaries).

## 6. Order of checks on the request

- **Decision**: (1) load account, (2) verify the account is accessible and the password is correct, (3) if the new email equals the current one answer `email_unchanged`, (4) if another account already holds the new email answer `account_already_exists`, (5) discard any earlier pending change, store the new one, send the mail.
- **Rationale**: Password first, so a caller who cannot prove the password learns nothing about which addresses are taken (SC-007). "Unchanged" before "duplicate" because the current email always exists in the table. Discarding the old pending change only after all checks pass means a failed request never destroys a good pending one.
- **Alternatives considered**: Duplicate check before password (leaks which addresses exist to anyone holding only a stolen access credential).

## 7. Confirm endpoint: verb, auth, and response

- **Decision**: `GET /api/v1/users/email-change/confirm?token=…`, public, response `200 {"status":"confirmed"|"already_confirmed"}`; invalid, expired, replaced, discarded, or blank tokens answer `400 invalid_or_expired_token`; a taken address answers `409 account_already_exists`.
- **Rationale**: The mailed link is opened in a browser, so it must be a `GET`. The registration confirm endpoint already works this way and returns the same status shape. The token proves ownership of the new mailbox (FR-023), so no access credential is needed, and the link must work from another device. A `GET` that changes state can be triggered by a mail scanner that prefetches links; that is harmless here because the token proves nothing beyond mailbox control, and a repeat answers `already_confirmed` rather than an error.
- **Alternatives considered**: `POST` confirm behind a front-end page (needs a front end that does not exist; out of scope); requiring a signed-in caller (breaks the cross-device link and adds a way for the confirm to fail for the legitimate owner).

## 8. Conflict at confirmation

- **Decision**: On confirm, look up the new address again. If another account holds it, discard the pending change and throw `DuplicateUserAccountException` (existing mapping to `409 account_already_exists`). The unique constraint on `normalized_email` stays the backstop for a race between the lookup and the save; a duplicate-key failure from `save` is handled the same way.
- **Rationale**: FR-024. The constraint already exists, so no new locking is needed.
- **Alternatives considered**: A distributed lock on the address (unneeded; the database constraint is the arbiter).

## 9. Suspended or locked account at confirmation

- **Decision**: Confirmation answers `invalid_or_expired_token` when the account is no longer accessible. The pending change is left to expire.
- **Rationale**: The endpoint is public. Answering "account cannot perform this action" would tell whoever holds the token something about the account's state. The person gets the generic answer and, once reinstated, can request a new change.
- **Alternatives considered**: `403 account_inaccessible` (leaks state to a public caller).

## 10. Username change without a password

- **Decision**: `PUT /api/v1/users/@me/username` with `{"username": "..."}`, `204`. `UserAccount.changeUsername(Username)` requires the account to be accessible (FR-013) and returns `false` when unchanged. Uniqueness is enforced by the existing unique constraint and mapped by the existing `DuplicateUserAccountException` handler.
- **Rationale**: Mirrors `PUT /users/@me/password`. No pre-check query is needed because the save already reports duplicates.
- **Alternatives considered**: `PATCH /users/@me` with a partial body (invites unrelated fields into this endpoint); a pre-check `findByUsername` (extra round trip and a race the constraint covers anyway).

## 11. Email request response

- **Decision**: `POST /api/v1/users/@me/email-change` answers `202` with no body when accepted.
- **Rationale**: Nothing has changed yet; `202 Accepted` says so, and matches `POST /auth/verification/resend`. The address held aside is not echoed (FR-032).
- **Alternatives considered**: `204` (would read as "done"); `200` with the pending address (echoes a value for no use).

## 12. Mail content and link

- **Decision**: A new template `email-change.html` and subject, sent through a new `EmailChangeNotifier` port with a `MailEmailChangeNotifier` adapter that reuses the same retry policy, sender address, and public base URL. `VerificationMailProperties` gains `emailChangeSubject` and `emailChangeConfirmPath` (default `/api/v1/users/email-change/confirm`); `VerificationLinkFactory` gains a second method.
- **Rationale**: Different wording ("confirm your new address", not "welcome") and a different path, but the same delivery mechanics. Extending the existing properties record keeps one `identity-access.verification.mail.*` block.
- **Alternatives considered**: Reusing the registration template (wrong copy, and the link would point at the registration confirm path); a separate properties class (a second `public-base-url` to keep in sync).

## 13. Testing without a real mailbox

- **Decision**: Core: a fake `EmailChangeNotifier` records `(email, token, expiresAt)`. Infrastructure: `MailEmailChangeNotifierTest` asserts recipient, subject, and that the link starts with the configured base and path and carries the token, using a mocked `JavaMailSender`. End to end: the existing in-memory `RecordingMailSender` captures the message, the test reads the token from it, then calls the confirm endpoint.
- **Rationale**: Covers everything the service controls. Whether a real provider delivers to a real inbox is a manual, one-time check (quickstart, step "Real delivery").
- **Alternatives considered**: A real SMTP round trip with a mailbox reader in CI (needs credentials and network; explicitly out of scope per the spec).

## 14. Stale email claim in existing access credentials

- **Decision**: No change. Access credentials keep the issue-time `email` claim until they expire; refresh issues a new one from the account.
- **Rationale**: Documented in the spec's Assumptions. Nothing on the server derives account state from that claim.
- **Alternatives considered**: Ending sessions on email change (explicitly ruled out by FR-033).
