# Data Model: Account-Deleted Outbox and Relay

## 1. Entities

| Entity | Lives in | Lifetime |
|---|---|---|
| Account-deleted announcement | `identity_access.outbox` | One transaction: inserted and deleted by the same deletion |
| Account-deleted message | Kafka topic `identityaccess.user.v1` | Topic retention |
| Relay position | Postgres replication slot `identity_access_outbox` plus Connect's offsets topic | Until the relay is reset |
| Relay credential | Postgres role named by `IDENTITYACCESS_DEBEZIUM_USERNAME` (value in `infrastructure/components/postgresql/.env`) | Until the role is dropped |
| Publication | Postgres publication `identity_access_outbox`, `FOR TABLE identity_access.outbox` | Until dropped |

## 2. Announcement table

`identity_access.outbox` (created by `V2__create_outbox.sql`):

| Column | Type | Null | Rule |
|---|---|---|---|
| `id` | `uuid` | no | Primary key. UUIDv7 from `UniqueStringIdGenerator`, converted to `uuid`. Equals `payload.eventId` |
| `aggregate_type` | `text` | no | `user` for this feature. Chooses the topic |
| `aggregate_id` | `text` | no | The user id as a string. Becomes the Kafka key |
| `event_type` | `text` | no | `UserSoftDeleted`. Becomes the `eventType` header |
| `payload` | `jsonb` | no | The message value, section 3 |
| `occurred_at` | `timestamptz` | no | The account's `deletedAt` |

No other index or constraint. The table is empty at rest, so there is nothing to look up. The primary key is also the replica identity, which Postgres requires for the row's `DELETE` to be published.

## 3. Payload (`UserDeletedMessage`, package-private record)

| Field | JSON type | Source |
|---|---|---|
| `eventId` | string (UUID) | same value as the row's `id` |
| `userId` | string | `UserId.value()` rendered as a string |
| `occurredAt` | string, ISO-8601 UTC with `Z` | the account's `deletedAt` |

Example: `{"eventId":"0197c2a4-...","userId":"123456789","occurredAt":"2026-11-01T08:15:30Z"}`. No email, username or birthday. Unknown fields may be added later; consumers must ignore them (spec 012 contract).

## 4. Core port

| Item | Definition |
|---|---|
| Name | `UserAccountEventPublisher` in `user.internal.port` |
| Method | `void publishDeleted(UserId userId, Instant deletedAt)` |
| Callers | `SoftDeleteUserAccountService`, inside `if (changed)`, after the account is saved and sessions are revoked |
| Failure | Any exception propagates and rolls back the whole deletion, so a failed announcement can never leave a committed deletion behind |

## 5. State and ordering

```text
handle(command)  [@Transactional]
  load account → account.close(...) sets DELETED and deletedAt  (or returns false)
  if changed:
      save account
      invalidate email-change verifications
      revoke all sessions
      publisher.publishDeleted(userId, account.deletedAt)    ← INSERT outbox, DELETE outbox
  commit  ──► change log carries: INSERT outbox, DELETE outbox   (rollback: nothing)
```

Concurrent deletes of one account: the account's version column lets one commit and the other fail, so at most one announcement is written. A repeat delete finds the account already `DELETED`, `changed` is false, and nothing is written.

## 6. Message mapping (by the relay)

| Message part | Source |
|---|---|
| Topic | `identityaccess.` + `aggregate_type` + `.v1` |
| Key | `aggregate_id` |
| Value | `payload`, expanded to JSON |
| Header `id` | `id` |
| Header `eventType` | `event_type` |
| Not published | The row's `DELETE`, and heartbeat records (they go to their own topic) |
