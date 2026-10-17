# Data Model: Identity Access Infrastructure Adapters

**Feature**: [spec.md](spec.md) | **Research**: [research.md](research.md)

Core models (`UserAccount`, `EmailVerification`, `AuthenticationSession`) are unchanged. This document describes only the **persistence-only** shapes owned by the adapters and how they convert. All persistence classes are package-private to their adapter package.

## Conventions

- **Instants** are stored with **microsecond** precision. Mappers truncate nanoseconds on write, so a round trip returns an equal value only for instants that already have at most microsecond precision (tests use such instants).
- **Digest** = lowercase hex SHA-256 of the presented token (`security/TokenDigest`). Raw tokens, raw refresh credentials, and passwords are never stored or logged.
- **Key prefix** `P` = `identity-access.redis.key-prefix`, default `identity-access:`.

## 1. PostgreSQL — schema `identity_access`

### `UserAccountRow` (write side) ↔ table `user_account` (migration `V1`, unchanged)

| Row field | Column | Type | Core source |
|---|---|---|---|
| `id` (`@Id`) | `id` | `BIGINT` | `UserId.value()` |
| `normalizedEmail` | `normalized_email` | `VARCHAR(320)` unique | `Email.value()` (already trimmed, lowercase) |
| `username` | `username` | `VARCHAR(32)` unique | `Username.value()` |
| `birthday` | `birthday` | `DATE` | `Birthday.value()` |
| `status` | `status` | `VARCHAR(32)` | `AccountStatus.name()`; checked against the 5 allowed values |
| `deletedAt` | `deleted_at` | `timestamptz`, nullable | `Instant` ↔ `OffsetDateTime` (UTC) |
| `passwordAlgorithm` | `password_algorithm` | `VARCHAR(64)` | `PasswordHash.algorithm()` |
| `passwordHash` | `password_hash` | `VARCHAR(512)` | `PasswordHash.hash()` |
| `passwordSalt` | `password_salt` | `BYTEA` | `PasswordHash.salt()` |
| `createdAt` | `created_at` | `timestamptz` | `Instant` ↔ `OffsetDateTime` |
| `updatedAt` | `updated_at` | `timestamptz` | `Instant` ↔ `OffsetDateTime` |
| `version` (`@Version`) | `version` | `BIGINT` | `UserAccount.version()`; `null` = new, 0 on first insert |

**Mapper** `UserAccountRowMapper`: `toRow(UserAccount)` and `toModel(UserAccountRow)` (uses `UserAccount.reconstitute`). The DB check constraint `ck_user_account_deletion` is mirrored by the core model; the mapper does not re-validate.

**Behaviour**
- `save` of a new account (version `null`) inserts; of a loaded account updates `WHERE id = ? AND version = ?`. A stale write raises `OptimisticLockingFailureException`.
- Unique violation on `normalized_email` or `username` → `DuplicateUserAccountException(message, cause)`.
- `findById/findByEmail/findByUsername` use indexed lookups on `id`, `normalized_email`, `username`.

### Read side — no persistence class

`JdbcUserAccountQueryRepository` selects `id, normalized_email, username, birthday, status, deleted_at, created_at, updated_at` and builds `FindUserByIdUseCase.Result` / `FindUserByEmailUseCase.Result` directly. `accessible` = `AccountStatus.valueOf(status).isAccessible()`.

### Schema ownership

| Object | Location |
|---|---|
| `user_account` and its indexes | schema `identity_access` |
| `flyway_schema_history` | schema `identity_access` |
| Anything in `public` or other services' schemas | none |

## 2. Redis — verification tokens

Source of truth for `EmailVerification`.

| Key | Type | Content | Expiry |
|---|---|---|---|
| `P verification:token:{digest}` | hash | `userId`, `email`, `createdAt`, `expiresAt`, `verifiedAt` (absent until consumed) — all numbers in micros, email as text | exactly `expiresAt` |
| `P verification:user:{userId}` | string | digest of the account's latest outstanding token | exactly `expiresAt` of that token |

`VerificationRecord` ↔ `EmailVerification` via `VerificationRecordMapper`. `toModel(record, presentedToken)` supplies the raw token the model requires from the presented value (the raw token is never read from Redis).

**Operations**
- `save`: write/overwrite the hash; set the user pointer when `verifiedAt` is absent; set expiry to `expiresAt`. A later `save` after consumption only adds `verifiedAt` and never extends expiry.
- `findByPresentedToken(token)`: blank → empty; otherwise `HGETALL` by digest. Unknown or expired → empty.
- `invalidateForUser(userId)` (Lua): read the pointer; if the pointed token exists and has **no** `verifiedAt`, delete it; delete the pointer. A consumed token stays readable until its expiry (spec 001 FR-004).

## 3. Redis — sessions

Source of truth for `AuthenticationSession`. All keys expire at `expiresAt + identity-access.session.retention` (default 30 days), so expired and revoked sessions stay readable while retained.

| Key | Type | Content |
|---|---|---|
| `P session:{sessionId}` | hash | `userId`, `deviceInfo`, `issuedAt`, `expiresAt`, `lastUsedAt` (optional), `revokedAt` (optional), `tokenDigest` |
| `P session:token:{digest}` | string | `sessionId` |
| `P session:user:{userId}` | sorted set | member `sessionId`, score `issuedAt` (micros) |

`SessionRecord` ↔ `AuthenticationSession` via `SessionRecordMapper` (`reconstitute`). The read adapter maps hashes straight to `FindSessionsQuery.SessionView`.

**Operations** (all Lua, `now` passed in as micros)

| Port method | Rule |
|---|---|
| `create(session, rawToken)` | Atomically write hash, token pointer, sorted-set entry and expiries. |
| `findByPresentedToken(raw)` | Pointer → hash. Returns expired and revoked records too. |
| `findBySessionId(id)` | Hash → model. |
| `rotate(id, presented, new, now, newExpiresAt)` | If hash exists, `revokedAt` absent, `now < expiresAt` and the presented credential's pointer names this session: delete that pointer, create the new pointer, set `tokenDigest`, `expiresAt = newExpiresAt`, `lastUsedAt = max(lastUsedAt, now)`, recompute key lifetimes (new expiry + retention), return true. Otherwise change nothing, false. One Lua script (`session-rotate.lua`). Replaces `touchIfUsable` (spec 004). |
| `revokeBySessionId(userId, id, now)` | If hash exists, owned by `userId`, `revokedAt` absent: set it, return true. In the same script also `SET <prefix>access-revoked:<userId>:<id> <now micros> PX <access-token-ttl>` (spec 004; revocation entry, nothing is written when the session was not newly revoked). |
| `revokeOtherSessions(userId, current, now)` | For every other retained session of the user with `revokedAt` absent: set it and write its revocation entry. Return count. |
| `revokeAllByUserId(userId, now)` | Same for every retained session (nothing kept). Return count. |
| `findByUserId(userId)` (view) | Read all ids in the sorted set, drop and prune ids whose hash expired, sort by `issuedAt` descending then `sessionId` ascending, map to `SessionView`. |

## 4. Non-persisted adapter configuration objects

| Object | Properties prefix | Notes |
|---|---|---|
| `JwtProperties` | `identity-access.security.jwt` | `private-key`, `public-key`, `issuer`, `audience`, `access-token-ttl` |
| `SecurityProperties` | `identity-access.security` | `public-endpoints`, `cors.allowed-origins` |
| `SnowflakeProperties` | `identity-access.snowflake` | `worker-id`, optional `epoch` |
| `SessionProperties` | `identity-access.session` | `lifetime` (exposed as the one `Duration sessionLifetime` bean), `retention` |
| `RedisKeyProperties` | `identity-access.redis` | `key-prefix` |
| `VerificationMailProperties` | `identity-access.verification.mail` | `from`, `public-base-url`, `confirm-path`, `subject` |

See [contracts/configuration.md](contracts/configuration.md) for defaults.

## 5. State transitions

None are added. Account, verification, and session lifecycles stay in core; adapters only persist them. The only adapter-level rules are expiry (verification: exact; sessions: plus retention) and the conditional updates in section 3.
