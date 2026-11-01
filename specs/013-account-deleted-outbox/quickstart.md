# Quickstart: Validating the Account-Deleted Outbox

Prerequisites: Docker, Java 21, the repo checked out, and real (non-placeholder) values for `IDENTITYACCESS_DEBEZIUM_USERNAME` and `IDENTITYACCESS_DEBEZIUM_PASSWORD` in `infrastructure/components/postgresql/.env` (copy the names from `.env.example`). Run Maven only through the service wrapper (`./mvnw` in `services/identity-access`), never a bare `mvn`. Commands assume the repo root unless a `cd` is shown. Contracts: [publisher-port](contracts/publisher-port.md), [outbox-record](contracts/outbox-record.md), [relay](contracts/relay.md).

## A. Without a broker (service only)

1. **Unit and architecture tests**
   ```bash
   cd services/identity-access && ./mvnw -q test
   ```
   Expect: the new core cases pass (published once on change, not on repeat, not on a wrong password); `PortAdapterMapTest` passes with the new row; `ForbiddenDependencyTest` still fails if a throwaway class imports `org.apache.kafka..` or is named `*Kafka*` (spec FR-011, US3).

2. **Change-log behaviour**: `JdbcUserAccountEventPublisherTest` and `AccountDeletionAnnouncementE2eTest` run against a Postgres Testcontainer with `wal_level=logical`. Expect: a committed deletion shows one `INSERT` and one `DELETE` for `outbox` in the decoded change log and the table ends empty; a rollback shows nothing; 100 repeated deletes of one account show one `INSERT`; a call with no transaction throws; the stored payload parses to exactly `eventId`, `userId`, `occurredAt` (SC-004, SC-005, SC-006).

## B. Platform bring-up

Order matters: the role script needs the `outbox` table, which the service creates when it first starts.

3. **Postgres with logical decoding**
   ```bash
   cd infrastructure/components/postgresql
   docker compose -p postgresql_chatter -f docker-compose.yml down
   docker compose -p postgresql_chatter -f docker-compose.yml up -d
   docker exec postgresql-chatter psql -U "$POSTGRES_USER" -d chatter -tAc "show wal_level"
   ```
   Expect `logical`. `./data` is kept.

4. **Service migrates once**: start `identity-access` (README of the service) so `V1` and `V2` run, then stop it if you like. Expect `identity_access.outbox` to exist and be empty.

5. **Relay role and publication**
   ```bash
   bash infrastructure/components/postgresql/scripts/create-relay-role.sh
   bash infrastructure/components/postgresql/scripts/create-relay-role.sh   # second run: no change, exit 0
   ```
   Expect: role with `rolreplication = true` and `rolsuper = false`; publication `identity_access_outbox` containing only `identity_access.outbox` (`SELECT * FROM pg_publication_tables`); `SELECT` granted on `outbox`; no password in the output. An empty or `change-me…` password makes it fail without changing anything, and so does running it before the service has migrated.

6. **Kafka, topics, relay**
   ```bash
   cd infrastructure/components/kafka
   source ../../shared/scripts/relay-env.sh
   docker compose -p kafka_chatter up -d
   bash scripts/create-topics.sh
   bash scripts/register-connectors.sh
   curl -s localhost:5004/connectors/identity-access-outbox/status
   ```
   Expect: `identityaccess.user.v1`, its `.profile-next.dlt` and `__debezium-heartbeat.identityaccess` exist and the legacy identity-access and Debezium topics are not created; connector and task `RUNNING`; running `register-connectors.sh` again updates without a duplicate; `curl localhost:5004/connectors/identity-access-outbox/config` shows the `${env:…}` placeholders and no password.

## C. End to end

7. **One deletion**: register a user, delete the account through the API, then read the topic:
   ```bash
   docker exec kafka-chatter kafka-console-consumer --bootstrap-server kafka-chatter:29092 \
     --topic identityaccess.user.v1 --from-beginning --max-messages 1 \
     --property print.key=true --property print.headers=true --timeout-ms 30000
   ```
   Expect within 30 seconds: key = user id; headers `id` and `eventType:UserSoftDeleted`; value a flat JSON object with exactly the three fields; no delete record, no empty record, no personal data (SC-003, SC-006).

8. **Broker down**: stop `kafka-chatter` and `kafka-connect-chatter`, delete another account (expect success at normal speed), start both, and expect that account's message to appear (SC-002, scenario US2-4).

9. **Relay restart**: stop only the Connect container, delete three accounts, start it again, and expect all three messages with none missing (US2-2).

10. **Idle position** *(verify R7)*: leave everything idle for an hour while another schema is written to, and compare before and after:
    ```sql
    SELECT slot_name, active, confirmed_flush_lsn,
           pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), confirmed_flush_lsn)) AS lag
    FROM pg_replication_slots WHERE slot_name = 'identity_access_outbox';
    ```
    Expect `confirmed_flush_lsn` to keep moving and `lag` to stay small (SC-008). If it does not, use the heartbeat-table fallback in research R7.

11. **Soak (not in CI)**: `bash infrastructure/components/kafka/scripts/soak-relay.sh 1000`. Expect `missing 0` and `leaked 0` (duplicates are allowed and reported) after the relay catches up (SC-001).

## D. Clean from scratch

12. Follow `kafka/docs/runbooks/relay.md` reset: delete the connector, drop slot and publication, remove Connect's internal topics, then repeat steps 5 to 7 and expect the same results (SC-009: under 30 minutes from a clean checkout).

## Results

Recorded 2026-11-01 on the local WSL stack (Postgres 17 `wal_level=logical`, Kafka 3.6 broker, Connect worker `quay.io/debezium/connect:3.7.0.Final`).

| Check | Outcome |
|-------|---------|
| Service tests (`identity-access`, `./mvnw verify`) | core 297, infrastructure 232, presentation 103, start 115 tests, 0 failures, 0 errors (includes `JdbcUserAccountEventPublisherTest`, `AccountDeletionAnnouncementE2eTest`, `PortAdapterMapTest`, `ForbiddenDependencyTest`). |
| Skeleton (`profile-next`, `./mvnw -q verify`) | passes, including `StartSmokeTest` (`/actuator/health` UP). |
| Relay state | connector and task `RUNNING`; `GET /connectors/identity-access-outbox/config` shows `${env:IDENTITYACCESS_DEBEZIUM_USERNAME}` and `${env:IDENTITYACCESS_DEBEZIUM_PASSWORD}` and no password. |
| One announcement (step 7) | key = user id; headers `id` and `eventType:UserSoftDeleted`; value is flat JSON with exactly `eventId`, `userId`, `occurredAt`; no tombstone. (Row written with the adapter's SQL shape from psql, not through the HTTP API.) |
| Broker down / relay restart (steps 8, 9) | messages for the accounts written while the broker or Connect was stopped all arrived after restart (earlier bring-up session). |
| Script reruns | `create-relay-role.sh`, `register-connectors.sh`, `create-topics.sh` each exit 0 a second time, one connector, no duplicate. |
| Soak, 1000 transactions with random broker and Connect outages (SC-001) | committed 900, rolled back 100, delivered (distinct) 900, duplicates 0, **missing 0, leaked 0**. PASSED. |

### Deviations and decisions

- **Heartbeat table (research R7 fallback) is in use.** Migration `V3__create_relay_heartbeat.sql`, the connector's `heartbeat.action.query` and the publication entry in `create-relay-role.sh` are in place. SC-008 below was measured with them. The slot was not measured without the heartbeat table, so these results do not show whether the plain `heartbeat.interval.ms` setting alone would have been enough.
- **First soak run failed because of the script, not the relay.** The final `docker start` raced a `docker stop` still in flight, so Connect stayed down for the whole wait (649 of 900 missing). The relay resumed from its stored LSN afterwards, so nothing was lost. `soak-relay.sh` now stops the chaos loop's child processes and waits until both containers report running (`ensure_running`); the rerun passed.
- **Build fix outside the feature's own files:** `services/identity-access/identityaccess-start/pom.xml` bound `spring-boot-maven-plugin:check` to `verify`, and Spring Boot 4.0.8 has no `check` goal, so `./mvnw verify` stopped there. It now binds `repackage`, as `profile-next` does. After that, `verify` passes for both services (identity-access: core 297, infrastructure 232, presentation 103, start 115 tests, 0 failures).
- **`ThrottlingE2eTest` is timing-sensitive.** It needs four sign-ins inside a 3-second window. It failed 4 of 12 on a loaded machine (load average about 2, swap in use) and passed 12 of 12 on a quiet one. The load explanation is likely but was not isolated further.

### SC-009: reset and bring-up of the relay (measured 2026-11-01)

Reset limited to the relay pieces (connector, replication slot, publication, `connect-configs`, `connect-offsets`, `connect-status`, `__debezium-heartbeat.identityaccess`); the Postgres data, `identity_access.outbox` (0 rows) and the topic `identityaccess.user.v1` were left alone. Then runbook steps 4 to 7 in order.

| Step | Result |
|------|--------|
| Reset, then role script, compose up, topics script, connector registration | connector and task `RUNNING` after **81 s** |
| A fresh announcement written and read from `identityaccess.user.v1` | arrived with key, headers `id` and `eventType:UserSoftDeleted`, flat JSON; **118 s** from the first reset command |
| Role | `rolreplication = t`, `rolsuper = f`; slot `identity_access_outbox` active |

Scope note: this is the relay reset on a machine that already has the images, volumes and `.env` values. It is not a fresh checkout, so it shows the runbook is short and repeatable, not the full from-nothing time.

### SC-008: retained WAL while idle (measured 2026-11-01, 15:35 to 16:35 UTC)

Outbox idle for the whole hour; one row updated every 30 s in a scratch schema (`relay_idle_probe`, dropped afterwards) so the database kept writing. The slot was sampled once a minute, 61 samples.

| Measure | Result |
|---------|--------|
| Slot `active` | true in all 61 samples |
| `confirmed_flush_lsn` | moved from `0/2F58FE8` to `0/2FEF2C0`; it advanced throughout |
| Retained WAL (current LSN minus `restart_lsn`) | 984 B to 67 KB after the first minute; 4 KB at the end |
| Lag behind current LSN | 192 B to 5 KB; 3.4 KB at the end |

Retained WAL stayed flat and tiny, so SC-008 is met with the heartbeat table in place. The raw samples were kept outside the repo.

### Success criteria status

| SC | Status | Evidence and limits |
|----|--------|---------------------|
| SC-001 | Met | Soak: 1,000 transactions, 900 committed, 100 rolled back, random broker and Connect outages. Delivered 900, missing 0, leaked 0, duplicates 0. |
| SC-002 | Partly measured | All 1,000 soak transactions were written successfully while the broker and Connect were repeatedly stopped, and the service path contains no broker code. Latency before and after the feature was not compared. |
| SC-003 | Met on observation | A fresh announcement was on the topic within the 5 s pause before the first read (limit 30 s). The soak backlog cleared within the 5-minute wait after outages ended. Neither delay was timed to the second. |
| SC-004 | Met | `JdbcUserAccountEventPublisherTest` and `AccountDeletionAnnouncementE2eTest` check the table is empty after each deletion. |
| SC-005 | Met | Core unit test loops 100 deletes of one account for one announcement; the e2e test fires two concurrent closes and sees one. |
| SC-006 | Met on a sample | Payload shape, key and both headers were checked in tests and on about ten live messages. Not checked across a large published set. |
| SC-007 | Met | Architecture tests (including the port-to-adapter map and the Kafka, Debezium and CDC name bans) pass in `./mvnw verify`, which now succeeds. |
| SC-008 | Met | See above: flat, small retained WAL over a 60-minute idle run, measured with the heartbeat table. |
| SC-009 | Partly measured | Relay reset and bring-up took 118 s, and every script repeats with exit 0. Not tried from a fresh checkout. |
