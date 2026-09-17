# User Account foundation model

## First-domain boundary

The first domain is the account record and authoritative accessibility state. It includes:

- `UserId`: positive typed ID, backed by `BIGINT`.
- `Email`: validated, normalized email.
- `Username`: validated username.
- `Birthday`: validated birth date.
- `PlainPassword`: input-only; never persist, log, or return it.
- `PasswordHash`: persisted algorithm, hash, and salt.
- `AccountStatus`: single accessibility state.
- `UserAccount`: account aggregate/model and its transition methods.

Include registration, account lookup/persistence, lock, unlock, disable, enable, soft-delete, and future-facing `activateAfterVerification`. Activation is a User Account API now; verification tokens and mail delivery are later work.

Exclude confirmation tokens, mail sending, login, access/refresh JWTs, sessions, password changes, and email/username/profile edits.

## Direct Spring Data JDBC model

Map `UserAccount` directly in `identityaccess-core` with `@Table`, `@Id`, `@Version`, and embedded password-hash columns. Use `@Embedded` only for multi-column `PasswordHash`.

Use custom JDBC read/write converters for each single-column value object, including `UserId`, `Email`, `Username`, `Birthday` where needed for its representation, and `AccountStatus`. Do not create a duplicate persistence entity/mapper. Infrastructure owns datasource, Flyway, converter configuration, PostgreSQL/repository wiring, the hash implementation, and the Snowflake generator implementation.

Core defines `UserIdGenerator`; infrastructure implements the selected Snowflake strategy. Preserve `BIGINT` IDs, not UUIDs/strings.

Direct JDBC storage includes normalized unique email, unique username, birthday, state, deletion timestamp, password algorithm/hash/salt, password-modified timestamp, audit timestamps, and optimistic-lock version. Database constraints/indexes are authoritative, including normalized-email and username uniqueness, non-null rules, and lookup indexes.

## Account state machine

This is the explicit default for the unanswered state decision. It replaces legacy independent `is_enabled`, `is_locked`, and `is_deleted` flags.

| Status | Accessible | Transitions |
| --- | --- | --- |
| `PENDING_VERIFICATION` | No | `ACTIVE` by future verification; `DISABLED` by disable; `LOCKED` by lock; `DELETED` by soft-delete |
| `ACTIVE` | Yes | `DISABLED`, `LOCKED`, or `DELETED` |
| `DISABLED` | No | `ACTIVE` by explicit administrative enablement; `LOCKED`; `DELETED` |
| `LOCKED` | No | `ACTIVE` by unlock; `DISABLED`; `DELETED` |
| `DELETED` | No | None |

- Registration creates inaccessible `PENDING_VERIFICATION`.
- Only future verification or explicit administrative enablement creates `ACTIVE`.
- `DISABLED`, `LOCKED`, and `DELETED` are inaccessible.
- Unlock returns `ACTIVE`, never pending.
- Disabling a pending account leaves it `DISABLED`.
- `DELETED` is terminal.

Soft delete anonymizes email and username, replaces credentials with a known non-usable password hash, records `deletedAt`, and frees original unique values. The record contains no original email, username, or usable password hash.
