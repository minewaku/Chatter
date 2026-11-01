# Contract: Relay and Platform Scripts

Requirements: spec FR-013 to FR-023. All paths are relative to the repo root. Option names were checked against the Debezium 3.7 documentation unless marked *(verify)*; the quickstart verifies those.

## 1. Postgres component

| Item | Value |
|---|---|
| `docker-compose.yml` | `command: ["postgres", "-c", "wal_level=logical"]` (already applied) |
| Applying it | `down` then `up -d`; data in `./data` is kept |
| Role script | `infrastructure/components/postgresql/scripts/create-relay-role.sh` |

### `create-relay-role.sh`

Run manually from anywhere, after `identity-access` has started once. It reads `IDENTITYACCESS_DEBEZIUM_USERNAME` and `IDENTITYACCESS_DEBEZIUM_PASSWORD` from the environment or, if unset, from `infrastructure/components/postgresql/.env` through `infrastructure/shared/scripts/relay-env.sh`. It talks to the database with `docker exec postgresql-chatter psql` as the admin user from the same `postgresql/.env`.

| Step | Behaviour |
|---|---|
| Preconditions | Fails with a clear message, changing nothing, if the container is not running, `wal_level` is not `logical`, either credential is empty or starts with `change-me`, or the table `identity_access.outbox` does not exist yet (start `identity-access` once so its migrations run) |
| Role | Creates the role `LOGIN REPLICATION` with the password, or alters the password if it exists. Never `SUPERUSER` |
| Grants | `CONNECT` on the database, `USAGE` on schema `identity_access`, `SELECT` on `identity_access.outbox` |
| Publication | `CREATE PUBLICATION identity_access_outbox FOR TABLE identity_access.outbox` if it does not exist; `ALTER PUBLICATION … SET TABLE identity_access.outbox` if it exists with other tables |
| Idempotence | A second run changes nothing and exits 0 |
| Output | Never prints the password |

## 2. Kafka component additions

### Compose service

| Item | Value |
|---|---|
| Service / container | `kafka-connect-chatter` |
| Image | `quay.io/debezium/connect:3.7.0.Final` |
| Depends on | `kafka-chatter` |
| Network | `chatter-net` |
| Host port | `127.0.0.1:5004` → container `8083` (REST) |
| `BOOTSTRAP_SERVERS` | `kafka-chatter:29092` |
| `GROUP_ID` | `connect-chatter` |
| Internal topics | `CONFIG_STORAGE_TOPIC=connect-configs`, `OFFSET_STORAGE_TOPIC=connect-offsets`, `STATUS_STORAGE_TOPIC=connect-status`, each with `CONNECT_*_STORAGE_REPLICATION_FACTOR=1` *(checked: the image maps `CONNECT_FOO_BAR` to `foo.bar`)* |
| Producer | `CONNECT_PRODUCER_ACKS=all`, `CONNECT_PRODUCER_ENABLE_IDEMPOTENCE=true` |
| Config provider | `CONNECT_CONFIG_PROVIDERS=env`, `CONNECT_CONFIG_PROVIDERS_ENV_CLASS=org.apache.kafka.common.config.provider.EnvVarConfigProvider` *(verify availability in the image)* |
| Environment passed | only `IDENTITYACCESS_DEBEZIUM_USERNAME` and `IDENTITYACCESS_DEBEZIUM_PASSWORD`, as `${…:?}` interpolations (message: set in `infrastructure/components/postgresql/.env`); compose reads them from the shell, so start the stack with `source infrastructure/shared/scripts/relay-env.sh && docker compose -p kafka_chatter up -d` |
| Health check | `GET /connectors` on the container's port 8083 |

### Connector definition

`infrastructure/components/kafka/connectors/identity-access-outbox.json` (the body of `PUT /connectors/identity-access-outbox/config`):

```json
{
  "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
  "plugin.name": "pgoutput",
  "database.hostname": "postgresql-chatter",
  "database.port": "5432",
  "database.dbname": "chatter",
  "database.user": "${env:IDENTITYACCESS_DEBEZIUM_USERNAME}",
  "database.password": "${env:IDENTITYACCESS_DEBEZIUM_PASSWORD}",
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
```

The file holds no secret. `topic.prefix` also names the heartbeat topic, `__debezium-heartbeat.identityaccess`. Delete events on the outbox table are filtered by the router, and `route.tombstone.on.empty.payload` stays at its default `false`.

### Scripts

| Script | Behaviour |
|---|---|
| `infrastructure/shared/scripts/relay-env.sh` (shared by the Postgres and Kafka components) | Sourced by the others. Reads only the two Debezium keys from `infrastructure/components/postgresql/.env` by parsing, never sources the whole file; exports them; fails if either is empty or starts with `change-me`; a non-empty value already in the environment wins |
| `scripts/register-connectors.sh` | Waits for the REST endpoint, then `PUT`s every file in `connectors/` to `/connectors/<name>/config` (create or update, so rerunning is safe) and prints the connector state |
| `scripts/create-topics.sh` | See Topics below |
| `scripts/soak-relay.sh [N]` | Outage soak for SC-001, default N=1000. Creates the topic `identityaccess.soak.v1`; runs N insert-then-delete transactions on `identity_access.outbox` with `aggregate_type='soak'` (every 10th rolled back, ids recorded to files), while a background loop randomly stops and starts `kafka-chatter` and `kafka-connect-chatter`; waits up to 5 minutes for the relay to catch up; reads the topic and compares the `id` headers. Exits non-zero if any committed id is missing or any rolled-back id appears; duplicates are reported but allowed. Deletes the soak topic at the end |

### Topics

`create-topics.sh` (idempotent, same helper as today):

| Topic | Partitions | Config |
|---|---|---|
| `identityaccess.user.v1` | 3 | defaults |
| `identityaccess.user.v1.profile-next.dlt` | 3 | defaults |
| `__debezium-heartbeat.identityaccess` | 1 | `cleanup.policy=delete`, short retention |

Removed from the script: `dev.shared.event.identityaccess.user`, `dev.internal.event.identityaccess.outbox`, `dev.internal.event.identityaccess.dlq`, and all `debezium-offsets.*` and `debezium-schema-history.*`. The profile and message `dev.*` entries are left.

## 3. Env files

- `infrastructure/components/postgresql/.env.example` gains `IDENTITYACCESS_DEBEZIUM_USERNAME=` and `IDENTITYACCESS_DEBEZIUM_PASSWORD=` (empty), with a comment that they are static credentials for the CDC relay, read by the role script and the Connect worker, and that empty or `change-me…` values are refused.
- `infrastructure/components/vault/.env.example` loses the two `IDENTITYACCESS_DEBEZIUM_*` lines: Vault serves only the Java services, and `configure-secrets.sh` would otherwise push them to `secret/identityaccess`. The `PROFILE_` and `MESSAGE_` Debezium lines belong to other services and are left alone. Gitignored local `.env` files are never edited by this feature.
- The role template `postgresql-role-orm.sql` is not changed (its `SUPERUSER` grant is a separate cleanup).

## 4. Runbook content (`kafka/docs/runbooks/relay.md`)

- Start order: set the two Debezium values in `postgresql/.env`; Postgres up with logical decoding; start `identity-access` once; role script; Kafka up (`source infrastructure/shared/scripts/relay-env.sh && docker compose -p kafka_chatter up -d`); topics script; register connector.
- Verify: connector and task `RUNNING`, a test deletion yields one message, key and headers as specified.
- Retained WAL check: `SELECT slot_name, active, pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), confirmed_flush_lsn)) FROM pg_replication_slots;` and what to do if it grows while the relay is down (restart the relay, or drop the slot and accept that announcements written while it was gone are lost).
- Reset: delete the connector, `SELECT pg_drop_replication_slot('identity_access_outbox')`, drop the publication, remove the Connect internal topics.
- Old topics: how to delete the three legacy identity-access topics by hand.
