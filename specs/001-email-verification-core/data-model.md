# Data Model: Registration Email Verification Core

**Scope**: Registration verification state in `identityaccess-core`. See [command contracts](contracts/commands.md) and [port contracts](contracts/ports.md).

## UserAccount (existing)

| Field | Type | Role |
|---|---|---|
| `id` | `UserId` | Stable account identity; returned as `long` only after successful verification. |
| `email` | `Email` | Current normalized registration email; must match the verification email. |
| `status` | `AccountStatus` | New registration starts `PENDING_VERIFICATION`; successful verification moves it to `ACTIVE`. Other states cannot be activated by this feature. |
| `updatedAt`, `version` | Existing fields | Existing update metadata; no new account field is required. |

The current `PENDING_VERIFICATION` state represents initial registration awaiting email verification. Protected account changes remain unavailable until activation. Email change is a separate feature and has no transition in this model.

## EmailVerification (new)

| Field | Type | Rule |
|---|---|---|
| `rawToken` | `String` | Opaque presented secret; absent from public results and logs. |
| `userId` | `UserId` | References one registered `UserAccount`. |
| `email` | `Email` | Address being proved; equal to the account's current normalized email at activation. |
| `createdAt` | `Instant` | Captured from the supplied `Clock` when issued. |
| `expiresAt` | `Instant` | `createdAt + configured Duration`; default duration is 24 hours. |
| `verifiedAt` | Optional `Instant` | Absent while unused; set on successful consumption. |

**Invariants**: Duration is positive, so `expiresAt > createdAt`. A presented token is expired when `now >= expiresAt`, whether used or unused. A used record retains `verifiedAt` and remains distinguishable until expiry. Raw-token storage is permitted by the core contract; adapter representation is deferred.

**Relationship**: An account may have historical verification records, but after issuance or replacement at most one outstanding token remains usable. Presented-token lookup resolves at most one record. Replacement makes earlier outstanding records unresolvable; it preserves consumed records for pre-expiry replay results. The port does not require an `invalidatedAt` model field.

## Lifecycle and Result Order

| Presented token state | Account condition | Result | Mutation |
|---|---|---|---|
| Blank, missing, or invalidated | Any | `InvalidOrExpired` | None |
| Expired, including already used | Any | `InvalidOrExpired` | None |
| Used and unexpired | Any | `AlreadyVerified` | None |
| Unused and unexpired | Missing, non-pending, or email mismatch | `InvalidOrExpired` | None |
| Unused and unexpired | Pending with matching email | `Verified(userId)` | Set `verifiedAt` and activate account together |

The later storage adapter must make the final transition single-use under concurrent presentation and roll back account/token writes together on failure.
