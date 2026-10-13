# Core Command Contracts: Registration Email Verification

These are Java core use-case contracts, not HTTP endpoints. Public verification types belong in `com.minewaku.chatter.identityaccess.emailverification.command`. Existing USER commands remain in their current packages.

## RequestEmailVerificationUseCase

| Item | Contract |
|---|---|
| Input | `Command(String email)` |
| Output | Fieldless `Accepted` |
| Validation | Use the existing `Email` value object; malformed input follows its validation failure. |
| Eligible account | The normalized email resolves to a `PENDING_VERIFICATION` account. Invalidate its earlier outstanding token, save one replacement, and request one delivery. |
| Other valid-email cases | Missing or ineligible account returns identical `Accepted` without generation or delivery. Outer-layer throttling remains outside core and must preserve the visible shape. |
| Failure | Repository, generator, or notifier failure propagates; no `Accepted` success is returned. |

`Accepted` contains no account ID, eligibility flag, token, or delivery status.

## VerifyEmailUseCase

| Item | Contract |
|---|---|
| Input | `Command(String token)` |
| Success | `Verified(long userId)` after the token is marked used and its matching pending account becomes active. |
| Invalid | `InvalidOrExpired` for blank, missing, invalidated, expired, missing-account, wrong-status, or email-mismatch cases. |
| Replay | `AlreadyVerified` for a used token while unexpired; no additional mutation. |
| Failure | Save failure propagates; account and token writes succeed or fail together. |

Precedence is missing/invalidated, expiry (`now >= expiresAt`), used, then account eligibility. Raw tokens never appear in results or logs. No public activation command accepts only a user ID.

## Existing Registration Command Effect

| Existing command outcome | Required effect |
|---|---|
| New `RegisterUserUseCase` registration | Save a pending account, issue one verification, request one delivery before `Registered(long userId)` returns. |
| `VerificationPending` | Preserve result; no new token or delivery. The separate replacement use case handles resend. |
| `AccountAlreadyExists` | Preserve result; no token or delivery. |
| Rejected registration | Preserve existing validation/rejection; no token or delivery. |

Registration invokes internal issuance directly rather than the public replacement use case. Email-change commands are outside this feature.
