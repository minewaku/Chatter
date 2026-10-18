# Contract: Core and Infrastructure Changes

Internal contracts between core and infrastructure that change in this feature. There is no external (HTTP or mail) change; see [unchanged-surface.md](unchanged-surface.md).

## Core: `VerificationPurpose`

```java
package com.minewaku.chatter.identityaccess.emailverification.internal.model;

public enum VerificationPurpose { REGISTRATION, EMAIL_CHANGE }
```

## Core: `EmailVerification`

Adds `purpose()`. Factories take the purpose as their first argument.

```java
static EmailVerification issue(VerificationPurpose purpose, String rawToken, UserId userId, Email email, Clock clock, Duration lifetime)
static EmailVerification reconstitute(VerificationPurpose purpose, String rawToken, UserId userId, Email email, Instant createdAt, Instant expiresAt, Instant verifiedAt)
```

Everything else (`isExpired`, `isVerified`, `verify`, `requirePositive`, the validation messages) is unchanged.

## Core port: `EmailVerificationRepository`

```java
public interface EmailVerificationRepository {
    /** Stores the proof under its own purpose's key family. */
    void save(EmailVerification verification);

    Optional<EmailVerification> findByPresentedToken(VerificationPurpose purpose, String token);

    /** Drops the user's outstanding unverified proof of this purpose. Proofs of other purposes are untouched. */
    void invalidateForUser(VerificationPurpose purpose, UserId userId);
}
```

- A blank or null token yields an empty result, as today.
- A token saved under one purpose MUST NOT be returned by `findByPresentedToken` for another purpose.
- `invalidateForUser` for one purpose MUST NOT remove the other purpose's proof for the same user.
- A verified proof is kept by `invalidateForUser`, as today.

`PendingEmailChangeRepository` is removed. Its `discardForUser` is `invalidateForUser(EMAIL_CHANGE, ...)`.

## Core service: `EmailVerificationIssuer` (new)

```java
@Service
public class EmailVerificationIssuer {
    public static final Duration LIFETIME = Duration.ofHours(24);

    /** Invalidates the user's earlier proof of this purpose, generates a token, stores the new proof, and returns it. Sends no mail. */
    public EmailVerification issue(VerificationPurpose purpose, UserId userId, Email email);
}
```

- Constructor: `(Clock, UniqueStringIdGenerator, EmailVerificationRepository)`, plus a second constructor taking a `Duration` for tests, mirroring the current coordinator.
- Order is fixed: invalidate, generate, issue, save. A failed generate or save after the invalidate behaves as it does in the coordinator today.
- `EmailVerificationIssuanceCoordinator.DEFAULT_LIFETIME` is removed; the one lifetime is `EmailVerificationIssuer.LIFETIME`.

## Core service: `EmailVerificationIssuanceCoordinator` (edited)

`issue(UserId, Email)` keeps its signature. It becomes `issuer.issue(REGISTRATION, ...)` followed by `notifier.sendVerification(email, rawToken, expiresAt)`. Constructor: `(EmailVerificationIssuer, VerificationNotifier)`.

## Core services in `user` (edited)

| Service | Change |
|---------|--------|
| `RequestEmailChangeService` | Constructor drops `PendingEmailChangeRepository`, `Clock`, `UniqueStringIdGenerator`; takes `EmailVerificationIssuer`. Calls `issuer.issue(EMAIL_CHANGE, account.id(), newEmail)`, then `notifier.sendEmailChange(...)`. Its `LIFETIME` constant is removed. The order of checks (password, unchanged, taken) and the `Result` are unchanged |
| `ConfirmEmailChangeService` | Reads with `findByPresentedToken(EMAIL_CHANGE, token)`; records use with `verify(now)` then `save`. The sequence (expired, already used, account usable, address equal, address taken, apply, save, record) and the three `Result` types are unchanged |
| `SoftDeleteUserAccountService` | Calls `invalidateForUser(EMAIL_CHANGE, id)` where it called `discardForUser(id)` |

`RegisterUserService`, `RequestEmailVerificationService` and `VerifyEmailService` pass `REGISTRATION` where they now touch the repository or the coordinator.

## Infrastructure: `RedisEmailVerificationRepository`

One class implements the port for both purposes.

| Purpose | Token key | User key |
|---------|-----------|----------|
| `REGISTRATION` | `<keyPrefix>verification:token:<sha256>` | `<keyPrefix>verification:user:<userId>` |
| `EMAIL_CHANGE` | `<keyPrefix>email-change:token:<sha256>` | `<keyPrefix>email-change:user:<userId>` |

- The key family comes from a private `switch` over `VerificationPurpose` with no default branch.
- The save and invalidate Lua scripts and their argument order are unchanged. The invalidate script still receives the token key prefix of the same purpose as its single argument.
- `VerificationRecord` is unchanged. `VerificationRecordMapper.toRecord(verification)` is unchanged in shape; `toModel(record, purpose, presentedToken)` gains the purpose.

## Infrastructure: removed

`adapter/outbound/emailchange/write/redis/{RedisPendingEmailChangeRepository, PendingEmailChangeRecord, PendingEmailChangeRecordMapper}.java`, and their tests `RedisPendingEmailChangeRepositoryTest`, `PendingEmailChangeRecordMapperTest`.

## Structure rules

`StructureRules.PORT_MAP` loses the row for `user.internal.port.repository.PendingEmailChangeRepository`. The row for `emailverification.internal.port.repository.EmailVerificationRepository` (adapter `RedisEmailVerificationRepository` in `verification.write.redis`) stays as it is. The `user.internal.port.EmailChangeNotifier` row stays.

## Test-support contract

`EmailVerificationTestFakes` (and its nested `Repository`, `Generator`) becomes `public` so `user` tests can build the same in-memory repository the registration tests use. The in-memory repository fake keys records by purpose and token, and its `invalidateForUser` removes only unverified records of that purpose and user.
