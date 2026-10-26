# Data Model: Session Endpoint Error Causes

No data changes: no table, column, Redis key, or script is added or edited. The user account and the authentication session keep their fields and lifetimes. What changes is which failure each lookup and rule raises.

## User account (rule only)

Status values and which are accessible are unchanged (`AccountStatus`: only `ACTIVE` is accessible).

```text
confirmCredentials(presentedPassword, hasher)
   │
   ├─ status not ACTIVE ─────────────────► InvalidAccountStateTransitionException   (password never compared)
   ├─ password fails the password policy ─► InvalidCredentialsException
   ├─ password does not match ────────────► InvalidCredentialsException
   └─ otherwise ──────────────────────────► returns
```

`close` runs only the last two steps (any status may close). `changePassword` and `authorizeEmailChange` run all three, as they do today, except that the policy check moves after the status check.

## Failure causes by use case

| Use case | Account row missing | Account not ACTIVE | Wrong password (incl. fails policy) | Current session unusable |
|---|---|---|---|---|
| List sessions | `UserAccountNotFoundException` | `InvalidAccountStateTransitionException` | n/a (no password) | n/a |
| End one session | `UserAccountNotFoundException` | `InvalidAccountStateTransitionException` | `InvalidCredentialsException` | n/a |
| End other sessions | `UserAccountNotFoundException` | `InvalidAccountStateTransitionException` | `InvalidCredentialsException` | `SessionUnusableException` |
| Change password / change email | unchanged (`IllegalArgumentException`, see research 4) | `InvalidAccountStateTransitionException` | `InvalidCredentialsException` | n/a |
| Close account | unchanged | not checked (any status may close) | `InvalidCredentialsException` | n/a |
| Sign-in | `AuthenticationFailedException` | `AuthenticationFailedException` | `AuthenticationFailedException` | n/a |
| Renewal | `AuthenticationFailedException` | `AuthenticationFailedException` | n/a | `AuthenticationFailedException` |

Order of checks on "end other sessions": reconfirmation limit, account row, account status, password, then current session. A wrong password therefore wins over an unusable session (spec User Story 5.2).

## Reconfirmation count

| Outcome of a confirmed action | Count after |
|---|---|
| Success | cleared |
| `InvalidCredentialsException` | kept (the attempt stays counted) |
| `InvalidAccountStateTransitionException`, `UserAccountNotFoundException`, `SessionUnusableException`, a conflict reached after the password was accepted | cleared |

## New exceptions

| Class | Package (core) | Extends | Message | Carries |
|---|---|---|---|---|
| `UserAccountNotFoundException` | `user.api.exception` | `RuntimeException` | fixed: "User account not found" | nothing |
| `SessionUnusableException` | `authentication.api.exception` | `RuntimeException` | fixed: "Session is not usable" | nothing |

Neither extends `IllegalArgumentException`, so neither can be caught by the generic `400` handler by accident.
