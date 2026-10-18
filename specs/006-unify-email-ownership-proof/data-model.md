# Data Model: Unify Email Ownership Proof

No table, column, or migration changes. The only persisted shape is the proof held in Redis, and its stored form is unchanged.

## Mailbox ownership proof (`EmailVerification`, extended)

Package `emailverification.internal.model`. One class now covers both former `EmailVerification` and `PendingEmailChange`.

| Field | Type | Rule |
|-------|------|------|
| `purpose` | `VerificationPurpose` | Required. Fixed at issue time. Not stored in the hash; it selects the key family |
| `rawToken` | text | Required, not blank. Held in memory only; Redis keeps only its SHA-256 digest |
| `userId` | `UserId` | Required. The account the proof belongs to |
| `email` | `Email` | Required. For `REGISTRATION` this is the account's current address. For `EMAIL_CHANGE` it is the new address being proven, and the account keeps its old one until the proof is verified |
| `createdAt` | instant | Required |
| `expiresAt` | instant | Required, strictly after `createdAt` |
| `verifiedAt` | instant, optional | When set, between `createdAt` (inclusive) and `expiresAt` (exclusive) |

Behavior, identical for both purposes:

- `issue(purpose, rawToken, userId, email, clock, lifetime)` stamps `createdAt` from the clock and `expiresAt = createdAt + lifetime`. A zero or negative lifetime is rejected.
- `reconstitute(purpose, rawToken, userId, email, createdAt, expiresAt, verifiedAt)` rebuilds a stored proof under the same validation.
- `isExpired(now)` is true at and after `expiresAt`.
- `isVerified()` is true once `verifiedAt` is set.
- `verify(now)` sets `verifiedAt` and returns true, unless the proof is already verified or expired, in which case it returns false and changes nothing.

Renames relative to the removed `PendingEmailChange`: `newEmail` is `email`, `confirmedAt` is `verifiedAt`, `isConfirmed` is `isVerified`, `confirm` is `verify`.

### State transitions

```text
issued ──verify(now), now < expiresAt──▶ verified
   │                                       │
   └──now ≥ expiresAt──▶ expired           └─ stays findable until expiresAt, reports "already used"
issued ──invalidateForUser(purpose, user)──▶ gone      (a verified proof is kept, as today)
```

## Verification purpose (`VerificationPurpose`, new)

| Value | Completes | Redis key family |
|-------|-----------|------------------|
| `REGISTRATION` | Activating an account in `PENDING_VERIFICATION` whose email equals the proof's email | `verification:` |
| `EMAIL_CHANGE` | Replacing an active account's email with the proof's email | `email-change:` |

A proof of one purpose is never visible through the other: its token and user keys live in the other family. Adding a purpose adds one value here and one key family in the repository's `switch`, and changes nothing for the existing values.

## Stored form (unchanged)

Prefixes below sit after the service key prefix `identity-access:`.

| Key | Holds | Lifetime |
|-----|-------|----------|
| `<family>token:<sha256(token)>` | Hash with `userId`, `email`, `createdAt`, `expiresAt`, and `verifiedAt` once used. Times are epoch microseconds | Expires on its own at `expiresAt` |
| `<family>user:<userId>` | The digest of the user's outstanding unverified proof, so a new proof or an account close can find and remove it | Expires with the proof it points to |

`<family>` is `verification:` or `email-change:`. A verified proof is not pointed to by the user key, so it is not removed by `invalidateForUser` and stays until it expires, which is what makes a repeated click answer "already verified".

## Relationships

- A proof belongs to exactly one user account (`userId`). An account has at most one unverified proof per purpose, enforced by the user key and the invalidate-then-save order in the issuer.
- The proof does not change the account by itself. `VerifyEmailService` (registration) and `ConfirmEmailChangeService` (email change) read it and decide the effect.

## Removed

`PendingEmailChange`, `PendingEmailChangeRepository`, `PendingEmailChangeRecord`, `PendingEmailChangeRecordMapper`, `RedisPendingEmailChangeRepository`. Their data lives on as `EmailVerification` with purpose `EMAIL_CHANGE`, in the same Redis keys.
