# Contract: Core Changes

Signatures show the shape the code must have after this feature; bodies are left to implementation.

## `UserAccount` (`user.internal.model`)

```java
/** Status, then password format, then password match. Throws InvalidAccountStateTransitionException or InvalidCredentialsException. */
public void confirmCredentials(String presentedPassword, PasswordHasher passwordHasher)

/** Throws InvalidAccountStateTransitionException(status, operation) unless the account is accessible. */
public void requireAccessible(String operation)

public boolean changePassword(String currentPassword, PlainPassword newPassword, PasswordHasher passwordHasher)   // was PlainPassword currentPassword
public boolean authorizeEmailChange(String currentPassword, Email newEmail, PasswordHasher passwordHasher)         // was PlainPassword currentPassword
public boolean close(String currentPassword, PasswordHasher passwordHasher)                                       // was PlainPassword currentPassword
```

- `verifyAccessibleCredentials` becomes `(String presentedPassword, PasswordHasher, String operation)` and calls `requireAccessible(operation)`, then `PlainPassword.presented(presentedPassword)`, then `passwordHasher.matches`.
- `close` skips `requireAccessible`.
- The three other inline `if (!isAccessible()) throw new InvalidAccountStateTransitionException(...)` checks in `changeUsername` and `applyVerifiedEmail` may call `requireAccessible` (optional tidy, no behavior change).
- `confirmCredentials` uses the operation text `"confirm current password"`.

## `PlainPassword`

```java
/** The password a caller presents to confirm an action. Input that fails the policy cannot be the real password, so it is a wrong password, not a malformed request. */
public static PlainPassword presented(String raw)   // throws InvalidCredentialsException("Current password is incorrect")
```

The constructor and its `IllegalArgumentException` are unchanged (used for new passwords).

## New exceptions

```java
package ...user.api.exception;
public final class UserAccountNotFoundException extends RuntimeException { public UserAccountNotFoundException() { super("User account not found"); } }

package ...authentication.api.exception;
public final class SessionUnusableException extends RuntimeException { public SessionUnusableException() { super("Session is not usable"); } }
```

## Services

| Class | Change |
|---|---|
| `AccountVerifier.verify(long, String)` | `guard.confirm(userId, ...)` runs: `findById(new UserId(userId)).orElseThrow(UserAccountNotFoundException::new)`, then `account.confirmCredentials(password, passwordHasher)`, then returns the account. No `try/catch` of `IllegalArgumentException`. |
| `FindSessionsService.handle` | `findById(...).orElseThrow(UserAccountNotFoundException::new)`, then `account.requireAccessible("list sessions")`. |
| `LogoutOtherSessionsService.handle` | The current-session `orElseThrow` throws `SessionUnusableException`. |
| `LogoutSessionService` | no change |
| `ChangePasswordService`, `RequestEmailChangeService`, `SoftDeleteUserAccountService` | pass `command.currentPassword()` (raw) to the entity method; stop calling `presentedPassword`. |
| `UserAccountCommandService` | remove `presentedPassword`. |
| `ReauthenticationGuard.confirm` | `catch (InvalidCredentialsException wrongPassword) { throw wrongPassword; }` then the existing `catch (RuntimeException accepted)` clears the count. Class Javadoc: wrong password only keeps the count. |
| `AuthenticationFailedException` | Javadoc: "Generic denial thrown by sign-in and renewal only; never reveals whether an account, password, or session exists." |
| `AuthenticateService`, `RefreshAccessTokenService` | no change |

## Presentation

| Class | Change |
|---|---|
| `ErrorCode` | add `SESSION_UNUSABLE(HttpStatus.UNAUTHORIZED, "The session is no longer usable.")` after `UNAUTHENTICATED`. |
| `ApiExceptionHandler` | delete the `ReauthenticationFailedException` handler; handler for `{InvalidAccountStateTransitionException, UserAccountNotFoundException}` returns `ACCOUNT_INACCESSIBLE`; new handler for `SessionUnusableException` returns `SESSION_UNUSABLE`. |
| `SessionController` | three `try/catch` blocks removed; `@DocumentedErrors` as in [error-answers.md](error-answers.md). |
| `AccountInaccessibleException`, `ReauthenticationFailedException` | deleted with their imports and test references. |
