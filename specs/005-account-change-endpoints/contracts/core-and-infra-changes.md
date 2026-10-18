# Core, Infrastructure, and Earlier-Spec Changes

## Core (`identityaccess-core`)

| Change | Detail |
|--------|--------|
| `UserAccount.changeUsername(Username)` | Drops the password parameters; throws `InvalidAccountStateTransitionException` unless accessible; `false` when unchanged |
| `UserAccount.close(PlainPassword, PasswordHasher)` | New. Verifies the password only (throws `InvalidCredentialsException`), then calls the existing no-argument `softDelete()`, which stays as the unconditional domain operation (core tests build closed accounts with it); fails the password check when already closed |
| `UserAccount.authorizeEmailChange(PlainPassword, Email, PasswordHasher)` | Replaces `changeEmail`: requires accessible and a correct password, returns `false` when the new email equals the current one, changes nothing |
| `UserAccount.applyVerifiedEmail(Email)` | Replaces the email after confirmation; requires accessible; `markUpdated` |
| `ChangeEmailUseCase`, `ChangeEmailService` | Deleted (nothing outside core and its tests uses them) |
| `ChangeUsernameUseCase.Command` | `(userId, newUsername)` |
| `SoftDeleteUserAccountUseCase.Command` | `(userId, currentPassword)` |
| `RequestEmailChangeUseCase`, `ConfirmEmailChangeUseCase` | New, see [data-model.md](../data-model.md) |
| `PendingEmailChange` | New model |
| `PendingEmailChangeRepository` | New port: `save`, `findByPresentedToken`, `discardForUser(UserId)` |
| `EmailChangeNotifier` | New port: `sendEmailChange(Email newEmail, String rawToken, Instant expiresAt)` |
| `SoftDeleteUserAccountService` | After a successful close also calls `discardForUser` (FR-027) |
| `DuplicateUserAccountException` | Adds a constructor taking only a message, for the pre-check |

## Infrastructure (`identityaccess-infrastructure`)

| Change | Detail |
|--------|--------|
| `RedisPendingEmailChangeRepository`, `PendingEmailChangeRecord`, `PendingEmailChangeRecordMapper` | New, package-private shapes, same style as the verification adapter, key prefix `email-change:` |
| `MailEmailChangeNotifier` | New, in the existing `verification.mail` package (it shares the package-private `VerificationMailException`, the link factory, and the properties); same retry policy and failure type as `MailVerificationNotifier`; failures never include the token or link |
| `VerificationMailProperties` | Adds `emailChangeSubject` (default "Confirm your new Chatter email") and `emailChangeConfirmPath` (default `/api/v1/users/email-change/confirm`) |
| `VerificationLinkFactory` | Adds `emailChangeLinkFor(rawToken)` |
| `email-change.html` | New template |
| `SecurityProperties.DEFAULT_PUBLIC_ENDPOINTS` | Adds `/api/v*/users/email-change/confirm` |
| `StructureRules` port table | Two rows: `PendingEmailChangeRepository` → `RedisPendingEmailChangeRepository` in `emailchange.write.redis`; `EmailChangeNotifier` → `MailEmailChangeNotifier` in `verification.mail` |
| Lua scripts | `verification-save.lua` and `verification-invalidate-for-user.lua` reused unchanged |

## Start (`identityaccess-start`)

| Change | Detail |
|--------|--------|
| `application.yml` | Public endpoint list gains the confirm path; `identity-access.verification.mail` gains `email-change-subject` and `email-change-confirm-path` |
| Tests | `AccountManagementE2eTest` updated for the confirmed close; new `EmailChangeE2eTest`; `SecretLeakageE2eTest` and `AllowListE2eTest` extended |

## Earlier specs to correct

| File | Change |
|------|--------|
| `specs/004-presentation-api/spec.md` | FR-016 and the endpoint list: close is now `POST /users/@me/close` with a password |
| `specs/004-presentation-api/contracts/http-api.md` | Replace the `DELETE /users/@me` row and its error list |
| `specs/004-presentation-api/quickstart.md` | Update any close step to the new form |
