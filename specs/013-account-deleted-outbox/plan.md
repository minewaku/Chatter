# Implementation Plan: Account-Deleted Outbox and Relay (identity-access)

**Branch**: `profile-next` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/013-account-deleted-outbox/spec.md`

## Summary

When `SoftDeleteUserAccountService` actually deletes an account, it calls a new core port, `UserAccountEventPublisher.publishDeleted(userId, deletedAt)`. The only adapter, `JdbcUserAccountEventPublisher`, inserts a row into `identity_access.outbox` and deletes it again in the same transaction (`Propagation.MANDATORY`), so the table is empty at rest and Postgres's change log carries the insert. A standalone Kafka Connect worker running the Debezium PostgreSQL connector reads the insert and the outbox event router turns it into one message on `identityaccess.user.v1`. Nothing in the service touches Kafka or Debezium. The work has four parts: service code, architecture-test and earlier-spec amendments, platform scripts (Postgres role and publication, Kafka topics), and the relay (Connect container plus connector definition). Decisions and rationale are in [research.md](research.md), shapes in [data-model.md](data-model.md), interfaces in [contracts/](contracts/), validation in [quickstart.md](quickstart.md).

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.0.8, so Jackson 3 under `tools.jackson`), Bash for scripts

**Primary Dependencies**: Service: Spring `JdbcClient` (already on the classpath through `spring-boot-starter-data-jdbc`), the Boot-provided Jackson `ObjectMapper`, `java-uuid-generator` through the existing `UuidV7IdGenerator`. No new service dependency. Platform: `quay.io/debezium/connect:3.7.0.Final` (Kafka Connect plus Debezium PostgreSQL connector and the outbox event router), the existing `confluentinc/cp-kafka:7.6.0`, `postgres:17`

**Storage**: PostgreSQL 17, schema `identity_access`; one new table, `outbox`, empty at rest

**Testing**: JUnit 5, AssertJ, ArchUnit, Testcontainers PostgreSQL (existing `PostgreSqlTestSupport`, now started with `wal_level=logical`), `test_decoding` logical slot to observe the change log without Debezium. Relay checks are scripted and manual (quickstart)

**Target Platform**: Linux containers on the shared `chatter-net` Docker network

**Project Type**: Multi-module Maven service (`core`, `infrastructure`, `presentation`, `start`) plus repo-level `infrastructure/` components

**Performance Goals**: A committed deletion reaches the topic within 30 seconds normally, 5 minutes after an outage (SC-003). The extra work in the deletion transaction is one insert and one delete

**Constraints**: No broker client or relay code in the service; deletion never depends on broker or relay; at-least-once delivery; the message carries no personal data; credentials never committed

**Scale/Scope**: Account deletions are rare (single digits per second at most). One event type, one topic, 3 partitions

## Constitution Check

`.specify/memory/constitution.md` is still the unfilled template, so there are no ratified gates. The binding rules for this work come from `AGENTS.md` (touch only what the task needs, leave sibling services alone) and from the structure rules of specs 003 and 012, which the architecture tests enforce.

| Rule | Status |
|---|---|
| Only relevant files are touched; sibling services unchanged | Pass: `identity-access` and repo-level `infrastructure/` only. `services/profile` untouched |
| Core has no persistence, web or messaging types (spec 003) | Pass: the port uses only `UserId` and `Instant` |
| Every core outbound port has exactly one adapter at a documented location (spec 003) | Pass: one `PORT_MAP` row is added |
| No broker client or relay code in `identity-access` (spec 012 FR-020) | Pass: kept by the retained Kafka, Debezium and CDC bans |
| Use cases are single-method handlers (spec 012 FR-016) | Pass: no new use case; the existing handler gains one collaborator |
| Persistence shapes are package-private (spec 003) | Pass: the payload record and SQL stay private to the adapter package |

Re-check after design: unchanged. No complexity justification needed.

## Decisions confirmed after analysis

- **Publication scope**: table-only, `CREATE PUBLICATION identity_access_outbox FOR TABLE identity_access.outbox`. No other table's rows reach the relay (FR-016). The role script therefore needs the table, so the order is: start `identity-access` once (migrations V1, V2), then run the script. See research R6.
- **Relay credentials**: `IDENTITYACCESS_DEBEZIUM_USERNAME` and `_PASSWORD` live only in `infrastructure/components/postgresql/.env`. Vault is not involved; the two keys leave `vault/.env.example`. Placeholder values (`change-me…`) are refused. See research R5.

## Project Structure

### Documentation (this feature)

```text
specs/013-account-deleted-outbox/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── publisher-port.md      # port, adapter, transaction rule, architecture-test changes
│   ├── outbox-record.md       # table, payload, message mapping
│   ├── relay.md               # Connect worker, connector definition, scripts, ports, env
│   └── amendments.md          # exact edits to spec 012, spec 003 and docs
├── checklists/requirements.md
└── tasks.md                   # produced by /speckit-tasks
```

### Source Code (repository root)

```text
services/identity-access/
├── identityaccess-core/src/
│   ├── main/java/.../user/internal/
│   │   ├── port/UserAccountEventPublisher.java                       # NEW
│   │   └── application/command/SoftDeleteUserAccountService.java    # +1 collaborator, call inside `if (changed)`
│   └── test/java/.../user/internal/application/
│       └── UserAccountApplicationServiceTest.java                    # constructor call sites + new cases
├── identityaccess-infrastructure/src/
│   ├── main/
│   │   ├── java/.../infrastructure/adapter/outbound/user/event/jdbc/
│   │   │   ├── JdbcUserAccountEventPublisher.java                    # NEW, public (Spring bean, port impl)
│   │   │   └── UserDeletedMessage.java                               # NEW, package-private payload record
│   │   └── resources/db/migration/V2__create_outbox.sql              # NEW
│   └── test/java/.../infrastructure/
│       ├── adapter/outbound/user/event/jdbc/JdbcUserAccountEventPublisherTest.java   # NEW
│       ├── structure/ForbiddenDependencyTest.java                    # drop the "Outbox" name ban only
│       ├── structure/StructureRules.java                             # +1 PORT_MAP row
│       └── support/PostgreSqlTestSupport.java                        # start container with wal_level=logical
└── identityaccess-start/src/test/java/.../start/e2e/
    └── AccountDeletionAnnouncementE2eTest.java                       # NEW

infrastructure/components/
├── postgresql/
│   ├── docker-compose.yml                                            # wal_level=logical (done)
│   ├── README.md                                                     # (done) + role script
│   ├── scripts/create-relay-role.sh                                  # NEW
│   └── docs/runbooks/relay-role.md                                   # NEW
├── kafka/
│   ├── docker-compose.yml                                            # + kafka-connect-chatter
│   ├── connectors/identity-access-outbox.json                        # NEW
│   ├── scripts/create-topics.sh                                      # drop legacy, add new
│   ├── scripts/register-connectors.sh                                # NEW
│   ├── README.md                                                     # + relay section
│   └── docs/runbooks/relay.md                                        # NEW: start, verify, reset, WAL watch
├── postgresql/.env.example                                           # + IDENTITYACCESS_DEBEZIUM_USERNAME / _PASSWORD (empty)
├── vault/.env.example                                                # the two Debezium keys removed (Vault serves only Java services)
├── kafka/scripts/soak-relay.sh                                       # NEW: 1,000-transaction outage soak (SC-001)
└── ../shared/scripts/relay-env.sh                                    # NEW (infrastructure/shared/scripts): reads the two Debezium keys from postgresql/.env

specs/003-infrastructure-adapters/ …                                  # amendment notes (see contracts/amendments.md)
specs/012-profile-next-rebuild/contracts/account-deleted-event.md     # corrections (see contracts/amendments.md)
docs/plans/identityaccess-infrastructure-migration.md                 # note
```

**Structure Decision**: The port lives in the `user` slice's `internal/port`, next to `PasswordHasher` and `EmailChangeNotifier`. The adapter sits under `adapter/outbound/user/event/jdbc/`, following capability → role → technology (`user`, `event`, `jdbc`), which keeps it apart from the account repository in `user/write/jdbc` because it has no model to map. Relay assets live in the existing Kafka component, and the Postgres role script lives in the existing Postgres component. No new top-level folder is created.

## Complexity Tracking

No constitution violations. Two choices are worth defending against "just use the existing pattern":

| Choice | Why needed | Simpler alternative rejected because |
|---|---|---|
| `JdbcClient` instead of a Spring Data JDBC row and repository | No core model to map; `payload` is `jsonb`; Spring Data JDBC `save` with an assigned id issues an update | A `Persistable` row, a mapper and a JSONB converter add three classes to write a row that is deleted milliseconds later |
| Kafka Connect worker as a new container | Debezium needs a host process; Connect registers connectors over REST and fits the existing Kafka component | Debezium Server needs its own properties file, has no REST registration, and does not match "registered by a script" (FR-019) |
