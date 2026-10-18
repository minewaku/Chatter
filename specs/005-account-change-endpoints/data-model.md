# Data Model: Account Change Endpoints

No PostgreSQL change. The only new stored state is the pending email change in Redis.

## Request and response shapes

| Name | Fields | Used by |
|------|--------|---------|
| `CloseAccountRequest` | `currentPassword` (required, 1–128, write-only) | close |
| `ChangeUsernameRequest` | `username` (required, 1–64; core's `Username` rules are the authority) | change username |
| `ChangeEmailRequest` | `newEmail` (required, email format, max 254), `currentPassword` (required, 1–128, write-only) | request email change |
| `EmailChangeStatusResponse` | `status`: `confirmed` or `already_confirmed` | confirm email change |

Every request record overrides `toString` to print only its type name, as the existing ones do, so `SecretHandlingTest` keeps passing without edits.

## Core use cases (public contracts)

| Use case | Command | Result |
|----------|---------|--------|
| `SoftDeleteUserAccountUseCase` | `(userId, currentPassword)` | `(changed)` |
| `ChangeUsernameUseCase` | `(userId, newUsername)` | `(changed)` |
| `RequestEmailChangeUseCase` (replaces `ChangeEmailUseCase`) | `(userId, currentPassword, newEmail)` | `(requested)`; `false` means the new email equals the current one |
| `ConfirmEmailChangeUseCase` | `(token)` | sealed: `Confirmed(userId)`, `AlreadyConfirmed`, `InvalidOrExpired` |

A taken address is signalled by throwing the existing `DuplicateUserAccountException`; a wrong password by the existing `InvalidCredentialsException`; a non-active account by the existing `InvalidAccountStateTransitionException`.

## PendingEmailChange (core model, `user.internal.model`)

> **Superseded by [spec 006](../006-unify-email-ownership-proof/spec.md):** this model was merged into `EmailVerification` (`emailverification.internal.model`) with a `VerificationPurpose`. See the data model of spec 006.

| Field | Meaning |
|-------|---------|
| `rawToken` | The emailed secret. Held in memory only; never stored |
| `userId` | The account the change belongs to |
| `newEmail` | The address awaiting proof of ownership |
| `createdAt`, `expiresAt` | Lifetime of 24 hours; expiry must be after creation |
| `confirmedAt` | Set once the token has been used; must fall inside the lifetime |

Rules: `isExpired(now)`; `isConfirmed()`; `confirm(now)` returns `false` if already confirmed or expired. It has the same invariants as `EmailVerification` and is a separate class so the two kinds can never be mixed.

## Redis entries (infrastructure only)

| Key | Type | Content | Lifetime |
|-----|------|---------|----------|
| `identity-access:email-change:token:<sha256(rawToken)>` | hash | `userId`, `email`, `createdAt`, `expiresAt`, `confirmedAt` (when used). Times are epoch microseconds, like `VerificationRecord` | expires at `expiresAt` |
| `identity-access:email-change:user:<userId>` | string | digest of the user's current pending token | expires at the same instant |

Written by the existing `verification-save.lua` and cleared by `verification-invalidate-for-user.lua` (the token key prefix is already an argument). `invalidate` skips a token that is already confirmed, so a confirmed record stays readable until expiry and a repeated confirm answers `already_confirmed`. The raw token is never part of the stored shape.

## Lifecycle

```text
none --(request accepted)--> pending(token T1)
pending(T1) --(new request)--> pending(T2)            T1 deleted
pending --(confirm, address free)--> confirmed        account email replaced; record kept until expiry
pending --(confirm, address taken)--> none            record deleted, 409
pending --(expiry)--> none                            entry disappears
pending --(account closed)--> none                    record deleted
confirmed --(confirm again)--> confirmed              200 already_confirmed
```

The account row changes at exactly one point: confirmation, where `email` and `updatedAt` are written. Before that, sign-in, mail, and the access-credential `email` claim use the old address.

## Account rules touched

| Operation | Accessible account required | Password required | Notes |
|-----------|-----------------------------|-------------------|-------|
| Close | No (any non-closed status) | Yes | Ends all sessions, discards pending change |
| Change username | Yes | No | Unchanged and duplicate outcomes as in the spec |
| Request email change | Yes | Yes | Order: password, unchanged, duplicate, store, mail |
| Confirm email change | Yes (otherwise `invalid_or_expired_token`) | No (token) | Re-checks the address, then replaces it |
