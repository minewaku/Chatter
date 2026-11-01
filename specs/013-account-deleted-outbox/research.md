# Research: Account-Deleted Outbox and Relay

Each entry is **Decision**, **Rationale**, **Alternatives**. Facts marked *(checked)* were read from the Debezium 3.7 documentation or the quay.io tag list on 2026-11-01. Items marked *(verify)* are expected to be right but must be confirmed by the quickstart because the docs were not fully readable.

## R1. Where the port lives and what it is called

- **Decision**: `user/internal/port/UserAccountEventPublisher` with `void publishDeleted(UserId userId, Instant deletedAt)`. Adapter `JdbcUserAccountEventPublisher` under `adapter/outbound/user/event/jdbc/`.
- **Rationale**: Matches the port naming in the `user` slice. "Publisher" is the familiar term and survives the later "access revoked" event as another method. The adapter's Javadoc says it writes a row that the relay picks up, so the name does not promise a broker call. `deletedAt` comes from the account (`UserAccount.softDelete()` already stamps it), so no second clock read and the message time equals the stored `deleted_at`.
- **Alternatives**: `UserAccountAnnouncer` (invented, not recognisable). A generic `OutboxWriter(aggregateType, id, eventType, json)` in core (leaks the table shape into core and invites other services to publish, which FR-028 puts out of scope). Writing the row from inside `UserAccountRepository.save` (hidden side effect on every save).

## R2. Adapter technology: `JdbcClient`

- **Decision**: Plain parameterised `INSERT` then `DELETE` through `JdbcClient`, in one method annotated `@Transactional(propagation = Propagation.MANDATORY)`.
- **Rationale**: There is no core model for the row, so the repo's row/mapper layer would map between two things that do not exist. Spring Data JDBC `save` decides insert or update from "is the entity new", and with an assigned id and no version it issues an `UPDATE` that fails. `jsonb` needs a converter there but only a `?::jsonb` cast here. `JdbcClient` uses the same `DataSource` as the Spring Data JDBC repositories, so it joins the same transaction. `MANDATORY` makes FR-007 (no write outside the deletion transaction) a runtime guarantee: the call throws `IllegalTransactionStateException` instead of writing a row nobody removes.
- **Alternatives**: Spring Data JDBC with `Persistable` (three extra classes). `JdbcTemplate` (older API, same result).

## R3. Table, ids and payload

- **Decision**: Table `identity_access.outbox` created by `V2__create_outbox.sql` with the columns from the contract (`id uuid pk`, `aggregate_type`, `aggregate_id`, `event_type`, `payload jsonb`, `occurred_at timestamptz`). Event id from the existing `UniqueStringIdGenerator` (UUIDv7), converted with `UUID.fromString` and bound as a `uuid`. Payload is a package-private record `UserDeletedMessage(eventId, userId, occurredAt)` serialised with the Boot-provided Jackson 3 `ObjectMapper` (`tools.jackson.databind`). The same `eventId` is in the row's `id` and in the payload.
- **Rationale**: Boot 4.0.8 ships Jackson 3, and the repo's tests already import `tools.jackson.databind`. Jackson 3 writes `Instant` as ISO-8601 by default; a test pins the exact JSON so a default change is caught. Using one id for row and payload lets consumers dedupe on either.
- **Alternatives**: Hand-built JSON string (escaping risk, no test value). `gen_random_uuid()` in SQL (v4, not time-ordered, and the payload would need a second round trip). Storing `user_id` as `bigint` (the contract says text, the Kafka key is a string, and Snowflake ids exceed 53 bits in JavaScript consumers).

## R4. Relay process: Kafka Connect, Debezium image 3.7.0.Final

- **Decision**: New compose service `kafka-connect-chatter` in the Kafka component, image `quay.io/debezium/connect:3.7.0.Final` *(checked: tag exists; docs call 3.7 the latest stable)*, REST port published on host `5004` (free: existing ports are 2181, 5001-5003, 5432, 5440-5442, 6380, 6387-6389, 8080, 9052) bound to `127.0.0.1`. Connect creates its own three internal topics with replication factor 1. Producer settings `acks=all` and `enable.idempotence=true` are set at worker level.
- **Rationale**: The Kafka component already exists, and Connect registers connectors by REST, which suits FR-019's "registered by a script". Worker-level producer settings avoid enabling the connector client override policy. The Kafka internal listener `kafka-chatter:29092` is reachable on `chatter-net`.
- **Alternatives**: Debezium Server (no REST, separate properties file, offsets stored in a file or Kafka topic of its own). Embedded Debezium in the service (banned by FR-020).
- *(checked locally, image pulled 2026-11-01)* `quay.io/debezium/connect:3.7.0.Final` exists. It runs Kafka Connect 4.3.1, contains `EventRouter` (in `debezium-connect-plugins-3.7.0.Final.jar`) and the PostgreSQL connector. Its entrypoint turns any `CONNECT_FOO_BAR` variable into the worker property `foo.bar`, so `CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR=1` and `CONNECT_PRODUCER_ACKS=all` work as written. `CONFIG_STORAGE_TOPIC`, `OFFSET_STORAGE_TOPIC` and `STATUS_STORAGE_TOPIC` are the image's own variables.
- *(verify)* Connect 4.3.1 clients against the Confluent 7.6.0 (Kafka 3.6) broker already in the compose file. Kafka 4.x clients should talk to any broker from 2.1 up; the quickstart will show it.

## R5. Credentials without Vault (Postgres component env file)

- **Decision**: The connector definition contains only placeholders `${env:IDENTITYACCESS_DEBEZIUM_USERNAME}` and `${env:IDENTITYACCESS_DEBEZIUM_PASSWORD}`, resolved inside the Connect worker by Kafka's `EnvVarConfigProvider`. The compose file passes just those two variables to the worker container. The two values live only in `infrastructure/components/postgresql/.env` (names in `.env.example`). `infrastructure/shared/scripts/relay-env.sh` reads them from there (parsing, not sourcing) and exports them for compose and for the role script; empty or `change-me…` values are refused. Connect stores the placeholder, not the secret, in its config topic.
- **Rationale**: Vault serves only the Java services, so the relay's credentials do not belong in Vault's env file (where `configure-secrets.sh` would also push them to `secret/identityaccess`). The Postgres component already owns the database admin credentials, and the role is created there. Passing a whole env file via `env_file` would hand unrelated secrets to the relay, so only the two variables are passed. The provider keeps the secret out of the committed JSON, out of REST responses, and out of the config topic.
- **Alternatives**: `envsubst` into the JSON before `POST` (the password then sits in the Connect config topic). `FileConfigProvider` with a mounted secrets file (one more file to manage).
- *(checked locally)* `org.apache.kafka.common.config.provider.EnvVarConfigProvider` is in the image's `kafka-clients-4.3.1.jar`, so no fallback is needed.

## R6. Relay role and publication

- **Decision**: `postgresql/scripts/create-relay-role.sh`, run manually, idempotent, executed through `docker exec` as the Postgres admin. It requires that the table `identity_access.outbox` already exists (the service creates schema and table on first start). It then: creates or alters the role `LOGIN REPLICATION` with the password from the environment; grants `CONNECT` on the database, `USAGE` on the schema and `SELECT` on `identity_access.outbox`; and creates the publication `identity_access_outbox` `FOR TABLE identity_access.outbox`, or `ALTER PUBLICATION … SET TABLE identity_access.outbox` if one already exists with other tables.
- **Rationale**: The script must not create the schema or table itself: the service's migrations create them as the dynamic Vault role, and objects owned by `admin` would stop that role working. A table-only publication means no other table's rows (`user_account` holds password hashes and salts) ever reach the relay process, which is what FR-016 requires. `SELECT` is part of the spec's credential definition (FR-016).
- **Trade-off (resolved)**: a schema-wide publication would let the script run before the table exists, but it streams every table in `identity_access` to the relay, which then discards the non-outbox rows. That conflicts with "capture no other table", so the table-only publication was chosen and the run order is service first, script second. The cost is that the heartbeat-table fallback in R7 also needs a database change (`ALTER PUBLICATION … ADD TABLE`).
- **Alternatives**: `publication.autocreate.mode=filtered` by the connector (needs `CREATE` on the database and table ownership for a role that should only read). A publication created by the Flyway migration (couples database replication objects to service releases).

## R7. Keeping the slot moving while idle

- **Decision**: `heartbeat.interval.ms=10000` and nothing else at first.
- **Rationale**: Debezium's documentation *(checked)* says heartbeats let the connector confirm the LSN so Postgres can recycle WAL. It also says that when the captured tables are idle but other tables in the same database write, the slot may still not advance, and `heartbeat.action.query` (a periodic write to a table that is in the publication) is the cure. This database is shared with other services, so SC-008 (flat retained WAL over an idle hour) must be measured, not assumed.
- **Fallback if SC-008 fails**: add table `identity_access.relay_heartbeat` through a migration, add it to the publication (`ALTER PUBLICATION identity_access_outbox ADD TABLE identity_access.relay_heartbeat`, done in the role script), and set `heartbeat.action.query` to update it.
- **Outcome (task T033)**: the fallback was implemented (`V3__create_relay_heartbeat.sql`, `heartbeat.action.query` on the connector, `relay_heartbeat` added to the publication in `create-relay-role.sh`). With it, an idle hour with another schema written every 30 s kept the slot active and retained WAL between 1 KB and 67 KB (SC-008, see quickstart "Results"). Not measured: the plain `heartbeat.interval.ms` setting without the table.
- *(verify)* quickstart step 7 compares `confirmed_flush_lsn` and WAL size over time.

## R8. Event router options

- **Decision**: `io.debezium.transforms.outbox.EventRouter` *(checked)* with `table.field.event.id=id` (emitted as header `id`), `table.field.event.key=aggregate_id`, `table.field.event.payload=payload`, `table.expand.json.payload=true`, `table.fields.additional.placement=event_type:header:eventType`, `route.by.field=aggregate_type`, `route.topic.replacement=identityaccess.${routedByValue}.v1`. Key converter `StringConverter`; value converter `JsonConverter` with `schemas.enable=false`; `tombstones.on.delete=false`.
- **Rationale**: `table.expand.json.payload` *(checked, default false)* is what turns the `jsonb` string into real JSON; without it the value would be an escaped string and the contract's "flat JSON" would be false. The router filters out `DELETE` operations *(checked)*, so the same-transaction removal never becomes a message. `route.tombstone.on.empty.payload` defaults to false *(checked)*, so no empty record either. The `id` header comes from the router itself.
- *(verify)* Connect leaves `${routedByValue}` alone because it has no `provider:` colon.

## R9. Snapshot mode and first start

- **Decision**: `snapshot.mode=no_data` *(checked: "never performs snapshots; streams from the slot's creation point")*. Slot `identity_access_outbox` and publication `identity_access_outbox` both named explicitly, `publication.autocreate.mode=disabled`.
- **Rationale**: The table is empty at rest, so a snapshot has nothing to copy. `no_data` is the current name; older docs call it `never`.

## R10. Topics

- **Decision**: `create-topics.sh` keeps its structure and creates `identityaccess.user.v1` (3 partitions, delete cleanup) and `identityaccess.user.v1.profile-next.dlt` (3 partitions), plus `__debezium-heartbeat.identityaccess` (1 partition). Removed: the three identity-access legacy topics (`dev.shared.event.identityaccess.user`, `dev.internal.event.identityaccess.outbox`, `dev.internal.event.identityaccess.dlq`) and all six `debezium-offsets.*` and `debezium-schema-history.*` topics. The environment prefix stays deployment configuration, outside the script.
- **Rationale**: Nothing in the new design reads those names. The Postgres connector writes no schema-history topic, and Connect keeps offsets in its own internal topics. The heartbeat topic is created explicitly so the script stays the single topic catalogue.
- **Kept on purpose**: the `dev.*` entries for `profile` and `message` (outbox, dlq, file-upload and socket topics). They belong to the legacy `profile` service (replaced by `profile-next` under spec 012) and to `message`, which is out of scope here, so each will be cleaned up in its own spec. Removing a name from the script never deletes an existing topic; the runbook says how to delete the old identity-access ones by hand.

## R11. Tests without a broker

- **Decision**: Run the Testcontainers Postgres with `wal_level=logical`, create a `test_decoding` logical slot, execute the adapter in a transaction, and read `pg_logical_slot_get_changes` to assert the insert and the delete (or nothing, on rollback). The end-to-end test goes through the real use case with a repeated delete.
- **Rationale**: This proves exactly what the relay depends on (what lands in the change log) with no Kafka or Debezium in the build. The relay itself is verified by the scripted quickstart, including a soak run for SC-001, which is too slow for CI.
- **Alternatives**: Testcontainers Kafka plus Debezium in CI (minutes of start-up, flaky on laptops).

## R12. Architecture-test changes

- **Decision**: In `ForbiddenDependencyTest`, remove only `haveSimpleNameContaining("Outbox")` and update the class comment. Keep the dependency ban on Kafka, Debezium, Vault and jjwt packages, the name bans (`Debezium`, `Cdc`, `Kafka`, `Vault`, the legacy filter names), and the pom assertions. Add the `PORT_MAP` row `user.internal.port.UserAccountEventPublisher → JdbcUserAccountEventPublisher @ user.event.jdbc`.
- **Rationale**: FR-010/FR-011. The service holds no broker client, so the bans on broker types remain the guard for FR-020. No Java class is named "Outbox" (the port and adapter use the publisher name), so removing the name ban is a clarification of intent, not a loophole.
