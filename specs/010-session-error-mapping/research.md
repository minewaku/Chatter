# Research: Session Endpoint Error Causes

Facts below were read from the code on 2026-10-27 (`AccountVerifier`, `UserAccount`, `PlainPassword`, `ReauthenticationGuard`, `UserAccountCommandService` and its subclasses, `FindSessionsService`, `LogoutSessionService`, `LogoutOtherSessionsService`, `SessionController`, `ApiExceptionHandler`, `ErrorCode`, and the presentation and core test classes that mention the affected exceptions). There were no unresolved clarifications in the Technical Context.

## 1. Where the confirmation rule lives

**Decision**: `UserAccount` owns the whole rule. A private `verifyAccessibleCredentials(String presentedPassword, PasswordHasher, String operation)` runs three steps in order: `requireAccessible(operation)`, then `PlainPassword.presented(raw)`, then `passwordHasher.matches`. A new public `confirmCredentials(String presentedPassword, PasswordHasher)` exposes it to the session commands. `changePassword`, `authorizeEmailChange` and `close` take the raw current-password string and use the same steps (`close` skips the status step, as today).

**Rationale**: The spec requires that a blocked account is answered `account_inaccessible` whatever password was sent (FR-004). Today the callers build a `PlainPassword` before the entity is consulted (`RequestEmailChangeService.java:59`, `SoftDeleteUserAccountService.java:40`, `ChangePasswordService.java:35`), so a malformed password on a blocked account would be judged first. Passing the raw string moves the status check ahead of the format check, inside one method, and `UserAccountCommandService.presentedPassword` (`:26-31`) disappears.

**Alternatives considered**:
- *Keep `PlainPassword` parameters and add `requireAccessible` calls in each service*: spreads the ordering across five callers, which is the duplication this feature removes. Rejected.
- *A `PresentedPassword` wrapper type that parses lazily*: one more type for one rule. Rejected as larger than the problem.
- *Leave the ordering as is (format first)*: a malformed password on a blocked account would return `reauthentication_failed`. It leaks nothing (the password policy is public), but it contradicts FR-004 as written. Rejected to keep the spec literal.

## 2. Weak current password on change-password

**Decision**: `ChangePasswordService` uses the shared rule, so a current password that fails the password policy is a wrong password (`reauthentication_failed`, `403`). It is no longer reported as `400 validation_failed`.

**Rationale**: `ChangePasswordRequest.currentPassword` only has `@NotBlank` and `@Size(max = 128)` (no `@Pattern`), and `ChangePasswordService.java:35` builds `new PlainPassword(current)`, whose `IllegalArgumentException` reaches the generic `400` handler. Email change and close already convert it to a wrong password (`presentedPassword`). Spec FR-005 and User Story 3 require the five actions to agree.

**Alternatives considered**: Keep `400` for change-password only. Rejected: it contradicts SC-003 and tells a caller the password "has invalid values" instead of "wrong", which leaks the policy shape for a field that is not being set.

## 3. Which exception reports "no such account"

**Decision**: A new core exception `UserAccountNotFoundException` in `user/api/exception`, mapped to `account_inaccessible`. Used by `AccountVerifier` and `FindSessionsService` when the repository returns nothing. A non-active account that exists still raises `InvalidAccountStateTransitionException` from `UserAccount.requireAccessible`.

**Rationale**: The two situations have different facts: a missing account has no status, so the existing exception (which requires an `AccountStatus`) does not fit it. They are two types because the facts differ, and one handler maps both to the same answer, which is what the spec asks (FR-003, FR-010). The name states the fact, not the HTTP outcome, so it stays correct if the mapping changes.

**Alternatives considered**:
- *One `AccountInaccessibleException` carrying an optional status*: one type, but half its callers would pass `null`, and it would overlap `InvalidAccountStateTransitionException`. Rejected.
- *Reuse `IllegalArgumentException("User account not found")` as `UserAccountCommandService.getAccount` does*: maps to `400 validation_failed` and is wrong for a caller with a valid credential. Rejected for the session endpoints.

## 4. `getAccount` in the account-change commands is not changed

**Decision**: `UserAccountCommandService.getAccount` (`:15-19`) keeps throwing `IllegalArgumentException` for a missing account. Not part of this feature.

**Rationale**: Spec scenarios for a missing account only cover the session endpoints (US1.5, US2.2). Reaching that case on an account-change endpoint needs a valid access credential for a row that no longer exists; deleting an account revokes its sessions, so this is not reachable in practice. Changing it would also alter `UserController`'s documented errors for a case with no test today.

**Alternatives considered**: Switch `getAccount` to the new exception for consistency. Deferred, to be done if a real path to it is found.

## 5. Attempt counting for the new outcomes

**Decision**: `ReauthenticationGuard.confirm` rethrows `InvalidCredentialsException` with the count kept and treats every other `RuntimeException`, including `InvalidAccountStateTransitionException` and `UserAccountNotFoundException`, as "accepted": it clears the count and rethrows. The catch clause no longer names `AuthenticationFailedException`.

**Rationale**: Today an inaccessible account through `AccountVerifier` raised `AuthenticationFailedException` and kept the count (`ReauthenticationGuard.java:41`). With the status check first, no password comparison happens for a blocked account, so there is nothing to guess and no reason to count (spec FR-008, SC-005). The same behavior already holds for change-password, which raised `InvalidAccountStateTransitionException`.

**Alternatives considered**: Keep counting blocked accounts. Rejected: it would lock a caller out of a legitimate retry after the account is reinstated, for no security gain.

## 6. The unusable current session

**Decision**: New core exception `SessionUnusableException` in `authentication/api/exception`, thrown by `LogoutOtherSessionsService` where it currently throws `AuthenticationFailedException` (`:43`). New `ErrorCode.SESSION_UNUSABLE(HttpStatus.UNAUTHORIZED, "The session is no longer usable.")`, wire code `session_unusable`. Mapped directly in `ApiExceptionHandler`. Added to `@DocumentedErrors` of `revoke-others` only.

**Rationale**: The clarified answer is `401` with its own code (spec Clarifications, FR-009). A distinct code lets a client tell it from `unauthenticated` (no or invalid credential) and from `authentication_failed` (sign-in or renewal denied). The message is fixed and does not say whether the session expired or belongs to someone else.

**Alternatives considered**: Reuse `UNAUTHENTICATED`. Rejected by the clarification: a client would not know the credential itself was fine and the session was the problem. Place the check before the password check. Rejected: the password is checked first so the session state is not disclosed to a caller who does not know the password (spec User Story 5.2).

## 7. `LogoutSessionService` needs no session check

**Decision**: `LogoutSessionService` only calls the verifier and then `revokeBySessionId`, which returns a boolean (`SESSION_NOT_FOUND` already covers an unknown target). It gets the new behavior through `AccountVerifier` and needs no edit of its own.

**Rationale**: Read at `LogoutSessionService.java:34-38`. The spec lists it as out of scope beyond using the new verifier.

## 8. Observability

**Decision**: No new logging. `ApiExceptionHandler` logs only the unexpected case, and none of the 4xx handlers log today.

**Rationale**: Adding a log line for only these causes would be inconsistent with the other handlers and the causes are now visible by status and code in access logs. This answers the one low-impact item left open after clarification.

**Alternatives considered**: A counter metric per cause. Deferred: no metrics are registered for errors elsewhere in the service.

## 9. Documents to update

**Decision**: Mark R4 in `specs/004-presentation-api/research.md` as superseded; update the rows at `specs/004-presentation-api/contracts/http-api.md` lines 90 and 101 and the entries at `specs/004-presentation-api/contracts/error-model.md` lines 26 and 41; replace the paragraph at `specs/007-abuse-throttling-resilience/contracts/http-surface.md` line 27 (it describes the `catch` and the wrapper that no longer exist); update the failure wording for the two commands in `specs/002-authentication-sessions/contracts/commands.md` (around line 46).

**Rationale**: Spec FR-013 and SC-007. The 007 paragraph cites `SessionController` line numbers that will also be stale.

## 10. Order of work

**Decision**: Core first (domain rule, exceptions, services, guard, their tests), then presentation (error code, handler, controller, deletions, tests), then end-to-end tests, then documents. Core and presentation each compile and pass on their own at the end of their step, because the presentation wrapper classes are deleted only after the controller stops using them.

**Rationale**: The reverse order would leave presentation catching an exception that core no longer throws.
