# Quickstart: Validating Session Endpoint Error Causes

Prerequisites: Docker running (the end-to-end tests start `postgres:17` and `redis:7` containers); Java 21. Build commands use the per-service wrapper, never bare `mvn`, from `services/identity-access`.

## 1. Core tests

```bash
cd services/identity-access
./mvnw -pl identityaccess-core -am verify
```

Expect:
- `UserAccountTest`: for `confirmCredentials`, `changePassword`, `authorizeEmailChange`, a non-active account raises `InvalidAccountStateTransitionException` whether the password is right, wrong, or fails the policy; an active account with a wrong or policy-failing password raises `InvalidCredentialsException`; `close` ignores status.
- `ReauthenticationGuardTest`: the count is kept only for `InvalidCredentialsException`; `InvalidAccountStateTransitionException`, `UserAccountNotFoundException` and `SessionUnusableException` clear it.
- `FindSessionsServiceTest`, `LogoutSessionServiceTest`, `LogoutOtherSessionsServiceTest`: the exceptions in [data-model.md](data-model.md); none throws `AuthenticationFailedException`; an unusable session on revoke-others revokes nothing; a wrong password wins over an unusable session.
- `AuthenticateServiceTest`, `RefreshAccessTokenServiceTest`: unchanged and green.

## 2. Presentation tests

```bash
./mvnw -pl identityaccess-presentation -am verify
```

Expect: `ApiExceptionHandlerTest` and `SessionControllerTest` match [contracts/error-answers.md](contracts/error-answers.md); `session_unusable` is `401`; no test references `ReauthenticationFailedException` or `AccountInaccessibleException`; the OpenAPI documented errors for the two password-confirmed session endpoints include `account_inaccessible`.

## 3. End-to-end

```bash
./mvnw -pl identityaccess-start -am verify
```

Expect, in `SessionManagementE2eTest`:
1. A signed-in caller with a wrong password on `revoke` and `revoke-others`: `403 reauthentication_failed`.
2. The same with a password that fails the policy: `403 reauthentication_failed`.
3. After the account is locked directly through the lock use case (there is no endpoint), with the same still-valid access credential: `GET /sessions`, `revoke`, `revoke-others` answer `403 account_inaccessible`, with the right password and with a wrong one.
4. After the current session is expired in the store: `revoke-others` with the right password answers `401 session_unusable` and no other session is revoked.
5. Sign-in and renewal failures still answer `401 authentication_failed` (existing tests, unchanged).

In `ThrottlingE2eTest`: repeated calls against a locked account never reach `429`, and a later wrong password after reinstating starts from a clean count.

## 4. Manual spot check (optional)

With the stack running, sign in, lock the account through the use case, then call `GET /api/v1/sessions` with the old access token and read the body: `{"status":403,"code":"account_inaccessible",...}`.

## 5. Formatting and documents

```bash
./mvnw spotless:check
```

Then confirm by search that no file under `services/identity-access` or `specs` still describes the removed wrappers as current behavior:

```bash
grep -rn "ReauthenticationFailedException\|AccountInaccessibleException" services/identity-access specs | grep -v "specs/010"
```

Remaining hits must be historical notes marked superseded by 010.
