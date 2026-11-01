# Contract: Account-Deleted Event

Producer: `identity-access` (through the standalone relay). Consumer: `profile-next`. Requirements: spec FR-011, FR-012, FR-019 to FR-025.

Illustrative values below show the intended shape. Option names were checked against Debezium 3.7 by spec 013 (*Amended by spec 013*); the connector actually deployed is `infrastructure/components/kafka/connectors/identity-access-outbox.json`.

## 1. Announcement record (identity-access database, table `identity_access.outbox`)

Written in the same transaction as the soft-delete, then deleted in that same transaction. The relay reads the insert from the change log, so the table is empty at rest.

| Column | Type | Meaning |
|---|---|---|
| `id` | uuid | Event id. Reaches consumers as the `id` header and is their dedupe key if they need one. |
| `aggregate_type` | text | Kind of record the event is about, `user`. Chooses the topic. |
| `aggregate_id` | text | The user id. Becomes the Kafka key. |
| `event_type` | text | `UserSoftDeleted`. Becomes a header. |
| `payload` | jsonb | The message value (section 2). |
| `occurred_at` | timestamptz | When the soft-delete happened. |

## 2. Message

| Part | Value |
|---|---|
| Topic | `identityaccess.user.v1`, one topic per kind of record and not per event type, so events about one user stay in order. The environment prefix is deployment configuration, not part of the name in code. |
| Key | the user id, as a string |
| Value | flat JSON, no wrapper |
| Headers | `id` (event id), `eventType` (`UserSoftDeleted`) |
| Partitions | 3 to start; ordering only matters per user id |
| Format | plain JSON, no schema registry |

```json
{ "eventId": "0197...", "userId": "123456789", "occurredAt": "2026-10-29T08:15:30Z" }
```

No personal data (email, username, birthday) is carried.

### Evolution

- Adding a field or a new `eventType` to the same topic is not a breaking change. Consumers MUST ignore unknown fields and unknown `eventType` values.
- A breaking change goes to a new topic, `identityaccess.user.v2`, published in parallel until consumers move.
- Each service owns its own copy of the shape. No shared library.

## 3. Relay (standalone Debezium process)

| Setting | Value |
|---|---|
| Source | the `identity-access` database only; capture the announcements table only |
| Slot and publication | one slot (`identity_access_outbox`), one publication (`identity_access_outbox`, `FOR TABLE identity_access.outbox`); the connector's table list repeats the restriction *(amended by spec 013)* |
| Credential | a dedicated role with replication and read access to the announcements table, static, supplied from the environment (not a rotating secret). Kept in the Postgres component's env file (`infrastructure/components/postgresql/.env`); Vault is not involved *(amended by spec 013)* |
| Routing | the outbox event router: key from `aggregate_id`, event id from `id`, payload from `payload`, `event_type` placed in the `eventType` header, topic from `aggregate_type` as `identityaccess.${routedByValue}.v1` |
| Idle position | a heartbeat of about 10 seconds so the slot keeps advancing while no announcements arrive |
| Producer | `acks=all`, `enable.idempotence=true` |
| Delivery | at least once; a repeat is possible after a crash |

```json
{
  "name": "identity-access-outbox",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "plugin.name": "pgoutput",
    "topic.prefix": "identityaccess",
    "slot.name": "identity_access_outbox",
    "publication.name": "identity_access_outbox",
    "publication.autocreate.mode": "disabled",
    "snapshot.mode": "no_data",
    "table.include.list": "identity_access.outbox",
    "heartbeat.interval.ms": "10000",
    "tombstones.on.delete": "false",
    "key.converter": "org.apache.kafka.connect.storage.StringConverter",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter.schemas.enable": "false",
    "transforms": "outbox",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.table.field.event.id": "id",
    "transforms.outbox.table.field.event.key": "aggregate_id",
    "transforms.outbox.table.field.event.payload": "payload",
    "transforms.outbox.table.expand.json.payload": "true",
    "transforms.outbox.table.fields.additional.placement": "event_type:header:eventType",
    "transforms.outbox.route.by.field": "aggregate_type",
    "transforms.outbox.route.topic.replacement": "identityaccess.${routedByValue}.v1"
  }
}
```

Database, host, port and credentials are supplied by environment, not committed. The definition lives with the other shared infrastructure and is registered by a script. The process is Kafka Connect with the Debezium PostgreSQL connector *(decided by spec 013)*. Without `table.expand.json.payload` the message value would be an escaped JSON string, not the flat JSON object this contract promises *(corrected by spec 013)*.

## 4. Consumer (`profile-next`)

| Behaviour | Rule |
|---|---|
| Group | `profile-next` |
| Handling | on `UserSoftDeleted`, delete the profile row; after commit, delete the stored images |
| Idempotence | deleting by user id is naturally idempotent; success when there is no profile |
| Unknown | ignore unknown fields and unknown `eventType` values |
| Failure | retry with exponential backoff, then publish to `identityaccess.user.v1.profile-next.dlt` and continue with the next record |
| Topics | `identityaccess.user.v1` and its `.dlt` are created ahead of the service, with the shared topic-creation script |

## 5. Not decided here

- How `identity-access` and `profile-next` organise their code for this (ports, packages, adapters). That is a plan and codebase decision.
- The "access revoked" signal for the `message` service (spec FR-028).
