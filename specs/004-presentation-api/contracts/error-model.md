# Error Model Contract

Every non-success response, from a controller or from the security chain, has this body (`application/json`):

```json
{
  "status": 400,
  "code": "validation_failed",
  "message": "The request contains invalid values.",
  "timestamp": "2026-10-17T10:15:30Z",
  "errors": [{ "field": "email", "message": "must be a well-formed email address" }]
}
```

`errors` appears only for `validation_failed`. `message` is fixed per code and never contains a submitted value, a core exception message, a class name, SQL, or a stack trace. `field` names are JSON property names.

## Codes

| Code | Status | Raised when | Source |
|------|--------|-------------|--------|
| `validation_failed` | 400 | Bean-validation failure or core value-object `IllegalArgumentException` | handler |
| `malformed_request` | 400 | Unreadable JSON or missing body | handler |
| `invalid_or_expired_token` | 400 | Verification token blank, unknown, invalidated, expired, or wrong account state | `VerifyEmailUseCase.InvalidOrExpired` |
| `unauthenticated` | 401 | Missing, malformed, expired, tampered, foreign-issuer, revoked, or `sid`-less access credential; non-numeric `sub` | entry point, filter |
| `authentication_failed` | 401 | Sign-in or renewal denied; one message for every cause | `AuthenticationFailedException` on endpoints 2 and 5 |
| `session_unusable` | 401 | The current session behind a valid access credential is expired or not the caller's, on endpoint 9 (added by [010](../../010-session-error-mapping/spec.md)) | `SessionUnusableException` |
| `reauthentication_failed` | 403 | Wrong current password, including one that fails the password rules, on endpoints 8, 9, 10 (amended by 010: no longer covers an inaccessible account or an unusable session) | `InvalidCredentialsException` |
| `account_inaccessible` | 403 | Authenticated account is missing, suspended, deleted, locked, or pending verification; reported before the password is compared (amended by 010) | `InvalidAccountStateTransitionException`, `UserAccountNotFoundException` |
| `forbidden` | 403 | Authorization denied by the chain (no current rule produces it) | access-denied handler |
| `session_not_found` | 404 | Session absent or not owned | `LogoutSessionUseCase.Result(false)` |
| `not_found` | 404 | No such path | handler |
| `method_not_allowed` | 405 | Wrong HTTP method | handler |
| `account_already_exists` | 409 | Duplicate email or username | `AccountAlreadyExists`, `DuplicateUserAccountException` |
| `verification_pending` | 409 | Account exists but is not yet verified | `VerificationPending` |
| `password_unchanged` | 409 | Password change was a no-op | `ChangePasswordUseCase.Result(false)` |
| `unsupported_media_type` | 415 | Body not JSON | handler |
| `internal_error` | 500 | Anything unexpected | handler (last resort) |
| `service_unavailable` | 503 | Revocation store could not be read on a protected request | entry point via `AuthenticationServiceException` |

## Mapping rules

1. `AuthenticationFailedException` is thrown only by sign-in and renewal and always maps to `authentication_failed` (401). (Amended by [010](../../010-session-error-mapping/spec.md); it was context-dependent in research R4, with controllers on endpoints 8-10 translating it.) Each other cause has its own exception, mapped directly by the handler. `session_unusable` is produced by the handler, so unlike the security-chain `401` answers it carries no `WWW-Authenticate` header.
2. Anything the handler does not recognise becomes `internal_error`. It logs the exception class and a correlation id (also returned nowhere in the body) and nothing from the request.
3. Identical-response requirement (SC-006): all causes under `authentication_failed` share status, code, and message.
4. `401` responses carry `WWW-Authenticate: Bearer`. `503` responses carry `Retry-After: 5`.
5. Security-chain responses are produced by presentation beans (`ApiAuthenticationEntryPoint`, `ApiAccessDeniedHandler`) so their body matches this contract exactly (SC-004).

## Logging rule

The handler and entry points log at most `code`, exception class, and correlation id. They never log request bodies, headers, query strings, or exception messages. A test sends sentinel values through every failure path and searches captured logs and responses for them (SC-005).
