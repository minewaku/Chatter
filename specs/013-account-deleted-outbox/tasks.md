---

description: "Task list for the account-deleted outbox and relay (identity-access), plus the empty profile-next service skeleton"
---

# Tasks: Account-Deleted Outbox and Relay (identity-access)

**Input**: Design documents from `/specs/013-account-deleted-outbox/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: Included. The spec's success criteria (SC-001 to SC-007) are verified by tests and by the quickstart, so test tasks come before or beside the code they cover.

**Organization**: Grouped by user story (US1 to US4 from the spec). Phase 7 is an addition requested outside the spec's stories: an empty `profile-next` service skeleton (label `[PN]`). It depends on nothing else here and nothing here depends on it.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no unfinished dependency)
- **[Story]**: US1 to US4, or PN for the skeleton phase

## Path Conventions

Paths are from the repo root. Abbreviations used below:

- `IA` = `services/identity-access`
- `IA-CORE` = `IA/identityaccess-core/src/main/java/com/minewaku/chatter/identityaccess`
- `IA-INFRA` = `IA/identityaccess-infrastructure/src/main/java/com/minewaku/chatter/identityaccess/infrastructure`
- `IA-INFRA-TEST` = `IA/identityaccess-infrastructure/src/test/java/com/minewaku/chatter/identityaccess/infrastructure`
- `IA-START-TEST` = `IA/identityaccess-start/src/test/java/com/minewaku/chatter/identityaccess/start`
- `INFRA` = `infrastructure/components`

Run Maven only through the service wrapper: `cd services/identity-access && ./mvnw …` (never a bare `mvn`). Do not commit unless asked.

---

## Phase 1: Setup

- [X] T001 Record the baseline: run `cd services/identity-access && ./mvnw -q test` and note any test that already fails before this feature (so later failures are not blamed on it). No file changes.
- [X] T002 [P] Confirm the relay image is local: `docker image inspect quay.io/debezium/connect:3.7.0.Final` (if missing, `docker pull quay.io/debezium/connect:3.7.0.Final`). Confirm `infrastructure/components/postgresql/docker-compose.yml` already contains `command: ["postgres", "-c", "wal_level=logical"]`.

---

## Phase 2: Foundational (blocks all user stories)

**Purpose**: test infrastructure that can observe the database change log, shared by US1 tests.

- [X] T003 [P] In `IA-INFRA-TEST/support/PostgreSqlTestSupport.java`, start the container with logical decoding: change the `POSTGRES` field to `new PostgreSQLContainer("postgres:17").withDatabaseName("chatter").withUsername("admin").withPassword("123456").withCommand("postgres", "-c", "wal_level=logical")`.
- [X] T004 [P] In `IA-START-TEST/e2e/E2eSupport.java` (the `POSTGRES` field at line 45), add `.withCommand("postgres", "-c", "wal_level=logical")` to the container definition in the same way.
- [X] T005 Create `IA-INFRA-TEST/support/ChangeLogProbe.java`: a small test helper over a `JdbcTemplate`/`DataSource` with `ChangeLogProbe.start(DataSource)` that runs `SELECT pg_create_logical_replication_slot('<unique-name>', 'test_decoding')`, `List<String> drain()` that runs `SELECT data FROM pg_logical_slot_get_changes('<unique-name>', NULL, NULL)` and returns the rows, and `close()` that drops the slot. Each decoded line looks like `table identity_access.outbox: INSERT: id[uuid]:'…'`; callers filter on the substring `identity_access.outbox`. Depends on T003.
- [X] T006 Update `IA-INFRA-TEST/support/JdbcTestConfiguration.java` so a Spring context built from it can create the new adapter: add `org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration` and `org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration` to the `@ImportAutoConfiguration` list, and add `@Import(UuidV7IdGenerator.class)` (class in `…infrastructure.adapter.outbound.shared.id`) next to `PostgreSqlConfiguration`. The existing `@ComponentScan("…adapter.outbound.user")` already covers the new `user.event.jdbc` package. If an import name does not resolve in Spring Boot 4.0.8, look for the class in the `spring-boot-jackson` and `spring-boot-transaction` jars on the Maven classpath and use the real name; the intent is a `tools.jackson.databind.ObjectMapper` bean, working `@Transactional` proxying, and a `UniqueStringIdGenerator` bean in that test context. Re-run `./mvnw -q -pl identityaccess-infrastructure -am test` and confirm the existing tests still pass.

**Checkpoint**: Tests can start Postgres with logical decoding and read its change log.

---

## Phase 3: User Story 1 - Deleting an account always leaves exactly one announcement (Priority: P1)

**Goal**: A committed soft-delete writes and removes one outbox row in its own transaction; a rollback, a repeat delete or a wrong password writes nothing; nothing outside the transaction can write.

**Independent Test**: Run the soft-delete use case against a real Postgres (no broker, no relay) and read the change log: committed deletion shows one INSERT and one DELETE on `identity_access.outbox` and the table is empty; rollback and repeat delete show nothing.

### Tests for User Story 1

- [X] T007 [P] [US1] Add core unit tests to `IA/identityaccess-core/src/test/java/com/minewaku/chatter/identityaccess/user/internal/application/UserAccountApplicationServiceTest.java`. Add a private static `RecordingPublisher implements UserAccountEventPublisher` that stores each `(UserId, Instant)` call. Tests: (a) a delete that changes the account calls `publishDeleted` exactly once with the account's id and its `deletedAt()`; (b) a second `handle` on the already-deleted account does not call it again; (c) a wrong password (`InvalidCredentialsException`) does not call it; (d) a loop of 100 `handle` calls for one account yields exactly one recorded call (SC-005). Compiles only after T011 and T012.
- [X] T008 [P] [US1] Create `IA-INFRA-TEST/adapter/outbound/user/event/jdbc/JdbcUserAccountEventPublisherTest.java`, extending `PostgreSqlTestSupport`, using a Spring context from `JdbcTestConfiguration` (T006), `TransactionTemplate`, and `ChangeLogProbe` (T005). Tests: (a) inside a committed transaction `publishDeleted(new UserId(123456789L), instant)` leaves `SELECT count(*) FROM outbox` = 0 and `drain()` contains exactly one `INSERT` and one `DELETE` line for `identity_access.outbox`; (b) in a transaction that calls `publishDeleted` and then throws (so it rolls back), `drain()` has no line for `identity_access.outbox` and the table is empty; (c) calling the adapter with no transaction throws `org.springframework.transaction.IllegalTransactionStateException` and `drain()` shows nothing; (d) the INSERT line carries `aggregate_type` `user`, `aggregate_id` `123456789`, `event_type` `UserSoftDeleted`, and the payload parses (use `tools.jackson.databind.ObjectMapper`) to exactly three fields: `eventId` equal to the row `id`, `userId` = `"123456789"` (a string), `occurredAt` = ISO-8601 UTC ending in `Z`. Use a fresh `drain()` per scenario so lines from one scenario do not leak into the next. Compiles only after T010 to T014.
- [X] T009 [P] [US1] Create `IA-START-TEST/e2e/AccountDeletionAnnouncementE2eTest.java`, extending the same base class as `AccountManagementE2eTest` and using the same registration, login and `POST /users/@me/close` helpers it uses (see that test around line 82). Before the call, create a `test_decoding` slot on `E2eSupport.POSTGRES` with plain `JdbcTemplate` SQL (the infra test helper is not visible from this module). Test: register and verify a user, delete the account through the API, drain the slot, and assert exactly one INSERT and one DELETE for `identity_access.outbox` whose INSERT payload contains this user's id, and that `SELECT count(*) FROM identity_access.outbox` is 0. Add a second test for the concurrent-delete edge case: register a second user, fire two `POST /users/@me/close` requests (body `{"currentPassword": …}`) with the same token at the same moment (two threads released by one `CountDownLatch`), drain the slot, and assert exactly one INSERT carrying this user's id (do not assert the second response beyond "not a second announcement"). Drop the slot in `@AfterEach`. Depends on T004 and T010 to T015.

### Implementation for User Story 1

- [X] T010 [US1] Create the migration `IA/identityaccess-infrastructure/src/main/resources/db/migration/V2__create_outbox.sql` with exactly: `CREATE TABLE outbox (id UUID PRIMARY KEY, aggregate_type TEXT NOT NULL, aggregate_id TEXT NOT NULL, event_type TEXT NOT NULL, payload JSONB NOT NULL, occurred_at TIMESTAMP WITH TIME ZONE NOT NULL);` (formatted one column per line, no other statements, no grants, no indexes). The table is created in `identity_access` by the module's Flyway default schema.
- [X] T011 [P] [US1] Create the core port `IA-CORE/user/internal/port/UserAccountEventPublisher.java`: `public interface UserAccountEventPublisher { void publishDeleted(UserId userId, Instant deletedAt); }` with a one-line Javadoc "Announces that the account was soft-deleted. Must run inside the deletion's transaction." Imports only `com.minewaku.chatter.identityaccess.user.internal.model.UserId` and `java.time.Instant`.
- [X] T012 [US1] Edit `IA-CORE/user/internal/application/command/SoftDeleteUserAccountService.java`: add a final field and a last constructor parameter `UserAccountEventPublisher publisher` (new order: `repository, passwordHasher, verifications, sessionRevocation, guard, publisher`), and inside `if (changed)`, after `sessionRevocation.revokeAllFor(account.id());`, add `publisher.publishDeleted(account.id(), account.deletedAt());`. Then update the nine `new SoftDeleteUserAccountService(…)` call sites in `UserAccountApplicationServiceTest.java` (lines 96, 146, 365, 368, 406, 427, 461, 481, 530) to pass a `RecordingPublisher` (created in T007) as the new last argument. Depends on T011.
- [X] T013 [P] [US1] Create the package-private payload record `IA-INFRA/adapter/outbound/user/event/jdbc/UserDeletedMessage.java`: `record UserDeletedMessage(String eventId, String userId, Instant occurredAt) {}` with a Javadoc saying it is the message value of `identityaccess.user.v1`, carries no personal data, and its field names are the contract (see `specs/013-account-deleted-outbox/data-model.md` section 3).
- [X] T014 [US1] Create `IA-INFRA/adapter/outbound/user/event/jdbc/JdbcUserAccountEventPublisher.java`: public, `@Component`, `implements UserAccountEventPublisher`, constructor takes `JdbcClient`, `UniqueStringIdGenerator` (core port `…shared.port`), and `tools.jackson.databind.ObjectMapper`. Method `publishDeleted` is annotated `@Transactional(propagation = Propagation.MANDATORY)`. Body: `UUID id = UUID.fromString(ids.nextId());` build `new UserDeletedMessage(id.toString(), String.valueOf(userId.value()), deletedAt)`; `String payload = objectMapper.writeValueAsString(message);` run `jdbc.sql("INSERT INTO outbox (id, aggregate_type, aggregate_id, event_type, payload, occurred_at) VALUES (:id, 'user', :aggregateId, 'UserSoftDeleted', CAST(:payload AS jsonb), :occurredAt)")` binding `id` (UUID), `aggregateId` (the user id as a string), `payload` (String), `occurredAt` (`deletedAt.atOffset(ZoneOffset.UTC)`), then `jdbc.sql("DELETE FROM outbox WHERE id = :id").param("id", id).update()`. No try/catch (any failure rolls back the deletion). Javadoc: writes a row that the change-capture relay reads from the database change log; contains no broker code. Depends on T010, T011, T013.
- [X] T015 [US1] Register the port in the architecture map: in `IA-INFRA-TEST/structure/StructureRules.java` add to `PORT_MAP` the entry `row("user.internal.port.UserAccountEventPublisher", "JdbcUserAccountEventPublisher", "user.event.jdbc")` (after the `EmailChangeNotifier` row). Required in the same change as T014 because `PortAdapterMapTest.everyCoreOutboundPortIsListedInTheAdapterMap` fails as soon as the port exists unmapped.
- [X] T016 [US1] Run `cd services/identity-access && ./mvnw -q test` and confirm: T007, T008 and T009 pass; `PortAdapterMapTest` passes; nothing from the T001 baseline regressed. Fix any failures before moving on.

**Checkpoint**: US1 is complete and testable with no broker or relay (SC-004, SC-005, SC-006 payload shape).

---

## Phase 4: User Story 2 - A standalone relay publishes each announcement to the broker (Priority: P1)

**Goal**: A Kafka Connect worker with the Debezium PostgreSQL connector reads the outbox insert and publishes one message on `identityaccess.user.v1` (key = user id, headers `id` and `eventType`, flat JSON value), resuming after outages.

**Independent Test**: With US1 in place, delete an account and read the topic: exactly one message with the right key, headers and value; stop and restart the relay and the broker and lose nothing.

- [X] T017 [P] [US2] Create `infrastructure/shared/scripts/relay-env.sh` (bash, `set -euo pipefail`, meant to be sourced): if `IDENTITYACCESS_DEBEZIUM_USERNAME` or `IDENTITYACCESS_DEBEZIUM_PASSWORD` is already set and non-empty keep it; otherwise read the key from `infrastructure/components/postgresql/.env` by parsing lines `KEY=VALUE` with `grep`/`cut` (never `source` the file, never print a value; that file is the only place these two values live and Vault is not involved); fail with a clear stderr message naming the key if a value is empty or starts with `change-me` (a placeholder), telling the user to set a real value in `infrastructure/components/postgresql/.env`; `export` both. Resolve the env path relative to the script location, not the working directory.
- [X] T018 [P] [US2] Create `infrastructure/components/postgresql/scripts/create-relay-role.sh` per `contracts/relay.md` section 1: source `infrastructure/shared/scripts/relay-env.sh` (T017); read admin user and database from `infrastructure/components/postgresql/.env` (`POSTGRES_USER`, `POSTGRES_DB`, default `chatter`); run all SQL through `docker exec -i postgresql-chatter psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"`. Steps: fail clearly (exit non-zero, no changes) if the container is not running, if `SHOW wal_level` is not `logical`, or if table `identity_access.outbox` does not exist (message: start identity-access once so its migrations run); create the role `LOGIN REPLICATION` (never `SUPERUSER`) or `ALTER ROLE … PASSWORD` if it exists, passing the password through a psql variable and `format('%L')` so it is never echoed; `GRANT CONNECT ON DATABASE`, `GRANT USAGE ON SCHEMA identity_access`, `GRANT SELECT ON identity_access.outbox`; `CREATE PUBLICATION identity_access_outbox FOR TABLE identity_access.outbox` if `pg_publication` has no such name, otherwise `ALTER PUBLICATION identity_access_outbox SET TABLE identity_access.outbox`. A second run exits 0 with no changes. Make the file executable.
- [X] T019 [P] [US2] Edit `infrastructure/components/kafka/scripts/create-topics.sh`: delete the `debezium-offsets.*` (3 lines) and `debezium-schema-history.*` (3 lines) blocks and the three identity-access lines (`dev.shared.event.identityaccess.user`, `dev.internal.event.identityaccess.outbox`, `dev.internal.event.identityaccess.dlq`); keep the `# Profile` and `# Message` blocks unchanged. Under a new `# Identity Access` heading add `create_topic "identityaccess.user.v1" 3`, `create_topic "identityaccess.user.v1.profile-next.dlt" 3`, and `create_topic "__debezium-heartbeat.identityaccess" 1 --config cleanup.policy=delete --config retention.ms=3600000`. Keep the existing `create_topic` helper and `--if-not-exists`.
- [X] T020 [P] [US2] Create `infrastructure/components/kafka/connectors/identity-access-outbox.json` with exactly the JSON body in `specs/013-account-deleted-outbox/contracts/relay.md` ("Connector definition"): the 24 keys including `database.user` = `${env:IDENTITYACCESS_DEBEZIUM_USERNAME}`, `database.password` = `${env:IDENTITYACCESS_DEBEZIUM_PASSWORD}`, `table.include.list` = `identity_access.outbox`, `transforms.outbox.table.expand.json.payload` = `true`, `snapshot.mode` = `no_data`, `publication.autocreate.mode` = `disabled`. It must contain no real credential.
- [X] T021 [US2] Add the Connect worker to `infrastructure/components/kafka/docker-compose.yml` as service and container `kafka-connect-chatter`: image `quay.io/debezium/connect:3.7.0.Final`, `depends_on: [kafka-chatter]`, network `chatter-net`, `ports: ["127.0.0.1:5004:8083"]`, `restart: unless-stopped`, environment `BOOTSTRAP_SERVERS=kafka-chatter:29092`, `GROUP_ID=connect-chatter`, `CONFIG_STORAGE_TOPIC=connect-configs`, `OFFSET_STORAGE_TOPIC=connect-offsets`, `STATUS_STORAGE_TOPIC=connect-status`, `CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR=1`, `CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR=1`, `CONNECT_STATUS_STORAGE_REPLICATION_FACTOR=1`, `CONNECT_PRODUCER_ACKS=all`, `CONNECT_PRODUCER_ENABLE_IDEMPOTENCE=true`, `CONNECT_CONFIG_PROVIDERS=env`, `CONNECT_CONFIG_PROVIDERS_ENV_CLASS=org.apache.kafka.common.config.provider.EnvVarConfigProvider`, and only the two variables `IDENTITYACCESS_DEBEZIUM_USERNAME: ${IDENTITYACCESS_DEBEZIUM_USERNAME:?set in infrastructure/components/postgresql/.env}` and `IDENTITYACCESS_DEBEZIUM_PASSWORD: ${IDENTITYACCESS_DEBEZIUM_PASSWORD:?set in infrastructure/components/postgresql/.env}`; health check `curl -sf localhost:8083/connectors` (interval 10s, retries 12). Do not use `env_file`. The variables are interpolated from the shell, so the stack is started with `source infrastructure/shared/scripts/relay-env.sh && docker compose -p kafka_chatter up -d` (run inside `infrastructure/components/kafka`).
- [X] T022 [US2] Create `infrastructure/components/kafka/scripts/register-connectors.sh`: source `../../../shared/scripts/relay-env.sh` (T017); wait until `http://localhost:5004/connectors` answers (up to 120 s, clear failure message); for each `connectors/*.json` run `curl -sf -X PUT -H 'Content-Type: application/json' --data @<file> http://localhost:5004/connectors/<file-name-without-.json>/config`; afterwards print `GET /connectors/<name>/status` (state of connector and tasks). Re-running updates instead of duplicating. Make the file executable.
- [X] T023 [P] [US2] Move the credential names to the Postgres component: append to `infrastructure/components/postgresql/.env.example` a comment (static credentials for the CDC relay, read by `scripts/create-relay-role.sh` and the Connect worker; empty or `change-me…` values are refused) followed by `IDENTITYACCESS_DEBEZIUM_USERNAME=` and `IDENTITYACCESS_DEBEZIUM_PASSWORD=` (both empty); and delete the two `IDENTITYACCESS_DEBEZIUM_*` lines from `infrastructure/components/vault/.env.example` (Vault serves only the Java services, and `configure-secrets.sh` would push them to `secret/identityaccess`). Leave the `PROFILE_` and `MESSAGE_` Debezium lines (other services). Do not edit any gitignored `.env`; in the final report tell the user to put real values in `infrastructure/components/postgresql/.env` and that the unused `IDENTITYACCESS_DEBEZIUM_*` lines in their local `vault/.env` may be deleted.
- [X] T024 [US2] Bring the relay up and verify it (quickstart steps 3 to 9): restart Postgres with the new compose setting and check `SHOW wal_level` = `logical`; start `identity-access` once so migrations V1 and V2 run; put real values for the two Debezium keys in `infrastructure/components/postgresql/.env`, run `create-relay-role.sh` twice; start the Kafka stack with `source infrastructure/shared/scripts/relay-env.sh && docker compose -p kafka_chatter up -d`, run `create-topics.sh` and `register-connectors.sh`; check connector and task `RUNNING` and that `GET /connectors/identity-access-outbox/config` shows the `${env:…}` placeholders and no password; delete an account and read `identityaccess.user.v1` (key, headers `id` and `eventType:UserSoftDeleted`, flat JSON with exactly `eventId`, `userId`, `occurredAt`, no tombstone); stop broker and Connect, delete an account, start both, and confirm the message arrives; stop only Connect, delete three accounts, start it, confirm all three messages. Record the results (and any deviation, for example a Connect 4.3.1 to Kafka 3.6 broker incompatibility, a rejected option name, or `${routedByValue}` being treated as a placeholder) in a new "Results" section at the end of `specs/013-account-deleted-outbox/quickstart.md`, and fix the connector JSON, compose file or `contracts/relay.md` if a setting had to change. Depends on T010 to T015 and T017 to T023.
- [X] T025 [P] [US2] Create `infrastructure/components/kafka/scripts/soak-relay.sh [N]` (executable, default N=1000) that proves SC-001 against the running relay without touching the real topic, per `contracts/relay.md` ("Scripts" table). (1) Create topic `identityaccess.soak.v1` (3 partitions). (2) In a temp directory generate a SQL file with N transactions; transaction i is `BEGIN; INSERT INTO identity_access.outbox (id, aggregate_type, aggregate_id, event_type, payload, occurred_at) VALUES ('<uuid>', 'soak', 'soak-<i>', 'SoakEvent', '{"eventId":"<uuid>","userId":"soak-<i>","occurredAt":"<now>"}'::jsonb, now()); DELETE FROM identity_access.outbox WHERE id = '<uuid>'; COMMIT;` with every 10th ending in `ROLLBACK;` instead, followed by `SELECT pg_sleep(0.02);`; generate each `<uuid>` in bash from `/proc/sys/kernel/random/uuid` and append it to `committed.txt` or `rolled-back.txt`. (3) Start a background chaos loop that every 3 to 8 seconds picks `kafka-chatter` or `kafka-connect-chatter` at random, runs `docker stop`, sleeps 2 to 6 seconds, and runs `docker start`. (4) Run the SQL with `docker exec -i postgresql-chatter psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"` (admin values from `infrastructure/components/postgresql/.env`). (5) Stop the chaos loop, make sure both containers are running, and wait up to 5 minutes for the relay to catch up. (6) Read the soak topic with `kafka-console-consumer --from-beginning --property print.headers=true --timeout-ms 60000` and collect the `id:` header values. (7) Print counts of committed, rolled back, delivered (distinct), duplicates (allowed), missing and leaked; exit non-zero if any committed id is missing or any rolled-back id appears. (8) Delete the soak topic. Needs the relay running (T024), but can be written in parallel with T017 to T023.

**Checkpoint**: US2 works end to end on the shared infrastructure (SC-002, SC-003, SC-006).

---

## Phase 5: User Story 3 - The service stays free of messaging code, and the tests prove it (Priority: P2)

**Goal**: Recording an announcement is allowed by the architecture tests; broker, Debezium and CDC code and names still fail the build.

**Independent Test**: The architecture tests pass with the new port and adapter; a throwaway class named after Kafka fails them.

- [X] T026 [US3] Edit `IA-INFRA-TEST/structure/ForbiddenDependencyTest.java`: in `noClassesNamedAfterDroppedFeatures` delete `.haveSimpleNameContaining("Outbox")` and make the next line (`.haveSimpleNameContaining("Debezium")`) the first condition after `.should()` (keep the rest of the `.orShould()` chain unchanged: `Debezium`, `Cdc`, `Kafka`, `Vault`, `JwtAuthenticationFilter`, `GlobalLoggingFilter`, `AccessTokenVerifier`). Replace the class comment with: `/** Spec 013 FR-010/FR-011: writing the announcement row is allowed; broker clients, Debezium, CDC code, Vault credential rotation and the legacy auth filters stay banned. */`. Leave `noMessagingVaultOrJjwtTypes` and `theModulePomDeclaresNoDroppedDependencies` untouched.
- [X] T027 [US3] Add a permanent negative check to `IA-INFRA-TEST/structure/ArchitectureRuleNegativeTest.java`: create the fixture `IA-INFRA-TEST/adapter/outbound/user/KafkaRelayLeak.java` (empty public class, Javadoc "fixture for the negative rule test") and a test `aClassNamedAfterKafkaIsStillRejected` that imports it with `ClassFileImporter().importClasses(KafkaRelayLeak.class)` and asserts `ForbiddenDependencyTest.noClassesNamedAfterDroppedFeatures.check(imported)` throws `AssertionError` mentioning `KafkaRelayLeak`. Add a second test `aClassNamedUserAccountEventPublisherIsAllowed` that imports `JdbcUserAccountEventPublisher` and asserts the same rule does not throw. The fixture lives in test sources, which the production rules exclude (`DoNotIncludeTests`).
- [X] T028 [P] [US3] Amend spec 003 per `specs/013-account-deleted-outbox/contracts/amendments.md`, "Spec 003" table: in `specs/003-infrastructure-adapters/spec.md` edit FR-024 (line 161) and SC-008 (line 186) with an "Amended by spec 013" note and the new wording; in `contracts/shared-components.md` replace the "Not set" table row; in `tasks.md` add "superseded by spec 013" to T077 (line 211); in `research.md` (line 130) and in `plan.md` where outbox or CDC is called excluded add the same note. Do not rewrite the history of those files, only add the notes.
- [X] T029 [US3] Run the architecture tests: `cd services/identity-access && ./mvnw -q -pl identityaccess-infrastructure -am test -Dtest='ForbiddenDependencyTest,ArchitectureTest,PortAdapterMapTest,ArchitectureRuleNegativeTest'`; confirm all pass. Then temporarily add a class `KafkaThing` to `IA-INFRA/adapter/outbound/user/event/jdbc/`, confirm `ForbiddenDependencyTest` fails, and delete the class again (leave nothing behind; `git status` must not list it).

**Checkpoint**: The boundary is enforced (SC-007).

---

## Phase 6: User Story 4 - A developer can bring up the whole path from a clean checkout (Priority: P2)

**Goal**: Documented, repeatable steps from a clean state to a delivered message, with reset.

**Independent Test**: Follow the runbooks on a machine with no relay state and finish with a message for a freshly deleted account.

- [X] T030 [P] [US4] Create `infrastructure/components/kafka/docs/runbooks/relay.md` per `contracts/relay.md` section 4: start order (real values for the two Debezium keys in `postgresql/.env`, Postgres with logical decoding, run `identity-access` once, role script, Kafka started with `source infrastructure/shared/scripts/relay-env.sh && docker compose -p kafka_chatter up -d`, topics script, `register-connectors.sh`), verification commands (connector status, console consumer with `print.key` and `print.headers`), the retained-WAL query on `pg_replication_slots` and what to do when it grows while the relay is down (restart the relay, or drop the slot and accept the loss), reset steps (`DELETE /connectors/identity-access-outbox`, `SELECT pg_drop_replication_slot('identity_access_outbox')`, `DROP PUBLICATION identity_access_outbox`, delete `connect-configs`, `connect-offsets`, `connect-status`), and how to delete the three legacy identity-access topics by hand with `kafka-topics --delete`.
- [X] T031 [P] [US4] Create `infrastructure/components/postgresql/docs/runbooks/relay-role.md`: what `create-relay-role.sh` does and its preconditions, where the credentials come from (`infrastructure/components/postgresql/.env`; Vault is not involved), re-running it after the service migrates, and how to drop the role, slot and publication.
- [X] T032 [US4] Update `infrastructure/components/postgresql/README.md` (add a "Relay role and publication" section linking `docs/runbooks/relay-role.md`, and the order: service first, then the script) and `infrastructure/components/kafka/README.md` (add `kafka-connect-chatter` on port 5004, the new scripts and `connectors/` folder in "Key Files", and links to `docs/runbooks/relay.md`; remove the mention of `assets/topics.json` only if that file does not exist).
- [X] T033 [US4] Prove the clean path (quickstart steps 10 to 12): run the reset from the relay runbook, repeat the bring-up from the runbooks only, and time it (target under 30 minutes, SC-009); re-run every script a second time and confirm exit 0 with no change; check `confirmed_flush_lsn` movement while idle (SC-008) and, if it does not advance while other schemas are written, implement the heartbeat-table fallback from `research.md` R7 (new migration `V3__create_relay_heartbeat.sql`, connector `heartbeat.action.query`, and `ALTER PUBLICATION identity_access_outbox ADD TABLE identity_access.relay_heartbeat` added to `create-relay-role.sh`) and record that decision in `research.md`; run `bash infrastructure/components/kafka/scripts/soak-relay.sh 1000` (T025) and record committed, delivered, duplicate, missing and leaked counts (SC-001 requires missing 0 and leaked 0). Add the outcomes to the "Results" section of `quickstart.md`.

**Checkpoint**: All four stories work independently.

---

## Phase 7: profile-next service skeleton (requested addition, label PN)

**Purpose**: Create `services/profile-next` as an empty clean-architecture multi-module service that mirrors `services/identity-access`, so spec 012 has somewhere to build. It contains no business logic, no outbox, no event publishing and no Kafka consumer; it only builds, passes its architecture tests, and starts. Artifact ids are lowercase like `identityaccess`: parent `profilenext`, modules `profilenext-core`, `profilenext-infrastructure`, `profilenext-presentation`, `profilenext-start`; Java package root `com.minewaku.chatter.profilenext`; default port `5011` (env `PROFILE_NEXT_SERVER_PORT`). `PN` = `services/profile-next`; `PN-CORE`, `PN-INFRA`, `PN-PRES`, `PN-START` abbreviate `PN/profilenext-<module>/src/main/java/com/minewaku/chatter/profilenext` with `…/src/test/java/…` for tests.

- [X] T034 [PN] Create the build scaffolding by copying from `services/identity-access`: `cp -a services/identity-access/mvnw services/identity-access/mvnw.cmd services/profile-next/`, `cp -a services/identity-access/.mvn services/profile-next/.mvn`, `mkdir -p services/profile-next/config && cp services/identity-access/config/eclipse-formatter.xml services/profile-next/config/`. Do not copy any `target/` folder.
- [X] T035 [PN] Create `services/profile-next/pom.xml` from `services/identity-access/pom.xml` with these changes: `artifactId` `profilenext`, `<name>`/`<description>` "Chatter Profile Next Parent Project", modules `profilenext-core`, `profilenext-infrastructure`, `profilenext-presentation`, `profilenext-start`; keep the Spring Boot parent 4.0.8, `java.version` 21, the Lombok `provided` dependency and the `maven-compiler-plugin` (`<release>` and `-parameters`) pluginManagement; remove the `spring-cloud.version` and `resilience4j.version` properties and the whole `dependencyManagement` block (not needed yet).
- [X] T036 [P] [PN] Create the core module: `services/profile-next/profilenext-core/pom.xml` (parent `profilenext`, `artifactId` `profilenext-core`, dependencies `spring-tx`, `spring-context`, and `spring-boot-starter-test` test scope, copied from `identityaccess-core/pom.xml`); `services/profile-next/profilenext-core/lombok.config` with the same three lines as `identityaccess-core/lombok.config` (`config.stopBubbling = true`, `lombok.accessors.fluent = true`, `lombok.addLombokGeneratedAnnotation = true`); and `PN-CORE/shared/handler/UseCaseHandler.java` identical to the identity-access one but in package `com.minewaku.chatter.profilenext.shared.handler` (`@FunctionalInterface public interface UseCaseHandler<C, R> { R handle(C command); }`). No feature slice (`profile`) is created here; spec 012 adds it.
- [X] T037 [P] [PN] Create the infrastructure module: `services/profile-next/profilenext-infrastructure/pom.xml` (parent `profilenext`; dependencies: `profilenext-core` `${project.version}`, `spring-boot-starter`, test-scope `spring-boot-starter-test` and `com.tngtech.archunit:archunit-junit5:${archunit.version}` with property `<archunit.version>1.5.1</archunit.version>`); plus a placeholder `PN-INFRA/infrastructure/adapter/outbound/package-info.java` containing only the package declaration and a Javadoc line "Outbound adapters, laid out capability → role → technology (see identity-access)". The placeholder makes `src/main/java` exist, which the logging scanner in T039 needs.
- [X] T038 [P] [PN] Create the presentation module: `services/profile-next/profilenext-presentation/pom.xml` (parent `profilenext`; dependencies: `profilenext-core`, `spring-boot-starter-webmvc`, test-scope `spring-boot-starter-test`, `spring-boot-starter-webmvc-test` and `archunit-junit5` with `<archunit.version>1.5.1</archunit.version>`); plus placeholder `PN-PRES/presentation/shared/package-info.java` (package declaration and Javadoc "Controllers and DTOs, one package per capability: presentation.<capability>.web and .dto"). No controllers.
- [X] T039 [PN] Create the infrastructure architecture tests in `PN/profilenext-infrastructure/src/test/java/com/minewaku/chatter/profilenext/infrastructure/structure/` by adapting the identity-access ones (package and root names `com.minewaku.chatter.profilenext`): `StructureRules.java` with `ROOT`/`INFRA` constants, `CORE_PACKAGES = {ROOT + ".profile..", ROOT + ".shared.."}`, an **empty** `PORT_MAP = List.of()`, and the rules `coreHasNoInfrastructureOrLibraryDependencies`, `coreDoesNotReferenceAdapters`, `persistenceShapesArePackagePrivate`, `noCatchAllFolders`, `assertPortMap`, `assertEveryCorePortIsMapped` copied from `identity-access` (drop the `com.nimbusds`, `jakarta.mail`, and `org.springframework.security` entries only if the skeleton has no such dependency; keep the rest) with `.allowEmptyShould(true)` added to every `noClasses()` rule that can match nothing; `ArchitectureTest.java` and `PortAdapterMapTest.java` copied with the new package names; `LoggingRuleTest.java` copied unchanged except the package. Add `ArchitectureRuleNegativeTest.java` with only the `coreImportingAnInfrastructureTypeIsRejected` case and its fixture `PN/profilenext-infrastructure/src/test/java/com/minewaku/chatter/profilenext/shared/fixture/ViolatingCoreModel.java` (a class in a core package, `com.minewaku.chatter.profilenext.shared.fixture`, with a field whose type is `com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.InfrastructureMarker`; create that marker as a public empty class in `PN-INFRA/infrastructure/adapter/outbound/InfrastructureMarker.java` with a Javadoc line saying it exists only so the negative architecture test has an infrastructure type to reference).
- [X] T040 [P] [PN] Create `PN/profilenext-infrastructure/src/test/java/com/minewaku/chatter/profilenext/infrastructure/structure/LegacyMachineryTest.java` (spec 012 FR-017, SC-007): an ArchUnit rule over `com.minewaku.chatter.profilenext` (`DoNotIncludeTests`, `allowEmptyShould(true)`) that fails for any class whose simple name contains `Outbox`, `Cdc`, `Debezium`, `DomainEvent`, `IntegrationEvent`, `EventDispatcher`, `AggregateRoot` or `AssetReference`. Do not ban `Kafka` names or packages: spec 012 adds a Kafka consumer here.
- [X] T041 [P] [PN] Create `PN/profilenext-presentation/src/test/java/com/minewaku/chatter/profilenext/presentation/structure/PresentationArchitectureTest.java` adapted from the identity-access one: same three rules with the package names `com.minewaku.chatter.profilenext.presentation`, forbidden packages `com.minewaku.chatter.profilenext.infrastructure..`, `com.minewaku.chatter.profilenext..internal..`, `org.springframework.data..`, `org.postgresql..`, `org.flywaydb..` (drop `io.lettuce` and `jakarta.mail`), and `.allowEmptyShould(true)` on each rule.
- [X] T042 [PN] Create the start module: `services/profile-next/profilenext-start/pom.xml` (parent `profilenext`; dependencies `profilenext-presentation`, `profilenext-infrastructure` with `${project.version}`, `spring-boot-starter`, `spring-boot-starter-actuator`, test-scope `spring-boot-starter-test`; `spring-boot-maven-plugin` with `mainClass` `com.minewaku.chatter.profilenext.start.StartApplication` and the `check` goal in `verify`, as in `identityaccess-start/pom.xml`); `PN-START/start/StartApplication.java` (`@SpringBootApplication(scanBasePackages = "com.minewaku.chatter.profilenext")` and `@ConfigurationPropertiesScan("com.minewaku.chatter.profilenext")`, with a `main` that calls `SpringApplication.run`); and `services/profile-next/profilenext-start/src/main/resources/application.yml` containing `server.port: ${PROFILE_NEXT_SERVER_PORT:5011}`, `spring.application.name: profile-next`, and `management.endpoints.web.exposure.include: health`. No datasource, Vault, Kafka or Redis settings.
- [X] T043 [PN] Create the start smoke test `PN/profilenext-start/src/test/java/com/minewaku/chatter/profilenext/start/StartSmokeTest.java`: `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`, inject the port with `@LocalServerPort`, call `GET /actuator/health` with `java.net.http.HttpClient`, and assert status 200 and a body containing `"status":"UP"`.
- [X] T044 [PN] Verify the skeleton: `cd services/profile-next && ./mvnw -q verify` must pass (core, infrastructure, presentation and start tests). Then run `cd services/profile-next && ./mvnw -q -pl profilenext-start -am spring-boot:run` in the background, confirm `curl -s localhost:5011/actuator/health` returns `{"status":"UP"}`, and stop the process. Finally `git status --short services/profile-next` must show no `target/` folders (they are ignored) and no file outside the list in T034 to T043. Check that port 5011 does not clash with anything in the repo (`grep -rn "5011" infrastructure services --include=*.yml --include=*.yaml --include=*.properties`).

**Checkpoint**: `services/profile-next` builds, its architecture tests pass, and it starts and answers `UP`.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T045 [P] Amend spec 012 per `contracts/amendments.md`: in `specs/012-profile-next-rebuild/contracts/account-deleted-event.md` fix the table schema (`identity_access.outbox`), the publication wording, the connector sample (`table.include.list`, payload expansion, converters, `publication.name`, `publication.autocreate.mode`, `snapshot.mode`, `topic.prefix`), the credential row, and replace "Kafka Connect or Debezium Server is decided in the plan" with the decision; in `specs/012-profile-next-rebuild/spec.md` reword the "one shared database instance" assumption and the relay bullet; note in `specs/012-profile-next-rebuild/checklists/requirements.md` that spec 013 closes the relay decision. Add "Amended by spec 013" beside each change.
- [X] T046 [P] Add one sentence to `docs/plans/identityaccess-infrastructure-migration.md` after line 6 saying the only messaging-related addition is the outbox row and its relay described in `specs/013-account-deleted-outbox/`, and that no legacy messaging, CDC or scheduler package is migrated.
- [X] T047 [P] Run `bash infrastructure/shared/scripts/normalize-line-endings.sh` (or the equivalent `sed -i 's/\r$//'`) on every new `.sh` file under `infrastructure/` and make sure each is executable (`chmod +x`), so they run under WSL/Linux.
- [X] T048 Run the full build for both services: `cd services/identity-access && ./mvnw -q verify` and `cd services/profile-next && ./mvnw -q verify`; confirm no regression against T001.
- [X] T049 Final review: `git status --short` and `git diff --stat`; confirm that nothing outside the files named in these tasks changed (in particular nothing under `services/profile`, `services/message`, `services/api-gateway`, `services/service-registry`), that no secret value appears in any committed file (`grep -rn "DEBEZIUM_PASSWORD" --include=*.json --include=*.yml --include=*.md .` should show only variable names), and tick off the SC-001 to SC-009 checklist in the "Results" section of `quickstart.md` with the measured values. Do not commit.

---

## Dependencies & Execution Order

### Phase dependencies

- **Phase 1 (Setup)**: none.
- **Phase 2 (Foundational)**: after Phase 1. Blocks Phases 3 to 6.
- **Phase 3 (US1)**: after Phase 2. T010 to T015 before T016; tests T007 to T009 are written first and compile only once T011 to T014 exist.
- **Phase 4 (US2)**: scripts and files (T017 to T023, plus the soak script T025) can start after Phase 1 and run alongside Phase 3; the bring-up and verification (T024) needs Phase 3 complete, and the soak run (inside T033) needs T024.
- **Phase 5 (US3)**: T026 to T028 can start after T015; T029 last.
- **Phase 6 (US4)**: T030 to T032 can be written once T017 to T022 exist; T033 needs T024.
- **Phase 7 (PN)**: independent of every other phase; can run in parallel with anything.
- **Phase 8 (Polish)**: T045 to T047 any time after the facts they describe are settled (T024); T048 and T049 last.

### Within a story

- Port (T011) → service change (T012) → payload record (T013) → adapter (T014) → map row (T015).
- Migration (T010) before the adapter test runs.
- `relay-env.sh` (T017) before the role script (T018) and register script (T022).

### Parallel opportunities

- Phase 2: T003, T004 together; T005 after T003.
- US1: T007, T008, T009 (three test files) and T011, T013 together.
- US2: T017, T018, T019, T020, T023 are all different files.
- US3: T028 (docs) beside T026 and T027.
- US4: T030 and T031 together.
- PN: T036, T037, T038 together; T040 and T041 together; the whole phase beside everything else.

### Parallel example: User Story 2

```text
T017 relay-env.sh        T018 create-relay-role.sh      T019 create-topics.sh
T020 connector JSON      T023 env example edits       T025 soak script
        └── then T021 compose service, T022 register script, T024 bring-up
```

---

## Implementation Strategy

### MVP first (User Story 1 only)

1. Phases 1 and 2, then Phase 3 (US1).
2. Stop and validate: the soft-delete writes and removes one outbox row, verified from the change log with no broker.
3. This alone satisfies FR-001 to FR-009 and FR-012; the relay (US2) is the next increment.

### Incremental delivery

1. US1 (the announcement) → test with the change log.
2. US2 (the relay) → test with a real message on the topic.
3. US3 (guardrails) → can land right after US1 since it only touches tests and docs.
4. US4 (runbooks and clean-path proof) → after US2 is verified.
5. PN (skeleton) → any time, independently.
6. Polish.

---

## Notes

- Decisions confirmed: the publication is table-only (`FOR TABLE identity_access.outbox`), so the role script runs after the service has migrated once; the Debezium credentials live only in `infrastructure/components/postgresql/.env` and Vault is not involved.
- `[P]` tasks touch different files and have no unfinished dependency.
- The skeleton phase adds no requirement to the spec; it is recorded under Assumptions in `spec.md`.
- Commit only when asked; end any commit message with the attribution line required for this session.
