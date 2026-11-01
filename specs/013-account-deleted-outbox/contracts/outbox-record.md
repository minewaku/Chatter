# Contract: Outbox Record and Message

Requirements: spec FR-004, FR-005, FR-009, FR-013, FR-015. Supersedes the table in `specs/012-profile-next-rebuild/contracts/account-deleted-event.md` section 1 where they differ (schema name). Field-level shape is in [data-model.md](../data-model.md).

## 1. Migration

`services/identity-access/identityaccess-infrastructure/src/main/resources/db/migration/V2__create_outbox.sql`, run by the service's own Flyway with `default-schema: identity_access`:

```sql
CREATE TABLE outbox (
    id             UUID PRIMARY KEY,
    aggregate_type TEXT NOT NULL,
    aggregate_id   TEXT NOT NULL,
    event_type     TEXT NOT NULL,
    payload        JSONB NOT NULL,
    occurred_at    TIMESTAMP WITH TIME ZONE NOT NULL
);
```

No other statements. No grants (the relay role gets `SELECT` from the platform script, not from the service).

## 2. Message on the wire

| Part | Value |
|---|---|
| Topic | `identityaccess.user.v1` (prefix added by deployment, not by code) |
| Key | user id as a string (`StringConverter`) |
| Value | `{"eventId":"…","userId":"…","occurredAt":"…Z"}`, real JSON, not an escaped string |
| Headers | `id` = event id, `eventType` = `UserSoftDeleted` |
| Partitions | 3 |
| Delivery | at least once |

The value is real JSON because the event router has `table.expand.json.payload=true` and the connector uses `JsonConverter` with `schemas.enable=false`. This corrects the spec 012 contract, whose connector sample would have produced an escaped string.

## 3. What must never appear

- The row's `DELETE` as a message, or as an empty record (a tombstone).
- Email, username, birthday or any other personal field in key, value or headers.
- Heartbeat records on the user topic.

## 4. Consumer-facing rules (unchanged from spec 012)

- Consumers ignore unknown fields and unknown `eventType` values.
- A breaking change goes to `identityaccess.user.v2`, published in parallel.
- Each service owns its own copy of the shape; there is no shared library.
