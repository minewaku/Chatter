# Core Port Contracts: Registration Email Verification

Ports are plain Java interfaces under `com.minewaku.chatter.identityaccess.emailverification.internal.port`. They do not name a storage system, mail provider, URL format, serialization format, or rate limiter.

## EmailVerificationRepository

| Operation | Contract |
|---|---|
| `save(EmailVerification verification) -> void` | Persist a new or consumed verification in the coordinated account transaction. |
| `findByPresentedToken(String token) -> Optional<EmailVerification>` | Resolve an outstanding or consumed record by its presented token. Return empty for missing or invalidated records; an expired record may be returned for the core time rule. |
| `invalidateForUser(UserId userId) -> void` | Make this account's earlier outstanding tokens unresolvable; preserve consumed records through expiry for `AlreadyVerified`. |

The core contract permits raw-token storage. A later adapter may use another representation if it preserves presented-token lookup and notification behavior. It must prevent ambiguous token lookup and prove real rollback and concurrent single use.

## VerificationTokenGenerator

`generate() -> String` supplies an opaque token for each issuance. Its later adapter must make tokens unpredictable. Core does not log the token or put it in public results.

## VerificationNotifier

`sendVerification(Email email, String rawToken, Instant expiresAt) -> void` requests delivery synchronously. It receives the exact token and expiry saved for that issuance. URL construction, templates, and provider behavior are outside core. Failures propagate; provider acceptance cannot be rolled back after a later commit failure.

## Shared Core Dependencies

The verification application reuses USER's `Email`, `UserId`, `UserAccount`, and `UserAccountRepository` (`findByEmail`, `findById`, `save`). A configurable positive `Duration` defaults to 24 hours; an injected `Clock` supplies issuance and expiry-check time. Command handlers coordinating writes carry `@Transactional`.
