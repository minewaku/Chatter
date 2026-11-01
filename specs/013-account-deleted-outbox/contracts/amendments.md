# Contract: Amendments to Earlier Work

Requirement: spec FR-024 (and FR-010, FR-020, FR-021, FR-022 where they touch other files). Each entry names the file, what is wrong now, and the replacement. Earlier specs keep their history: amend by adding an *Amended by spec 013* note beside the changed text instead of rewriting it silently.

## Spec 012 contract: `specs/012-profile-next-rebuild/contracts/account-deleted-event.md`

| Where | Now | Change to |
|---|---|---|
| Section 1, title line | "Announcement record (identity-access database)" | Add the schema: table is `identity_access.outbox` |
| Section 3, table row "Slot and publication" | "one publication limited to the announcements table" | "one publication, `identity_access_outbox`, `FOR TABLE identity_access.outbox`; the connector's table list repeats the restriction" |
| Section 3, sample JSON | `"table.include.list": "public.outbox"` | `"identity_access.outbox"` |
| Section 3, sample JSON | no payload expansion | add `"transforms.outbox.table.expand.json.payload": "true"`, `key.converter` `StringConverter`, `value.converter` `JsonConverter` with `schemas.enable=false`, `publication.name`, `publication.autocreate.mode=disabled`, `snapshot.mode=no_data`, `topic.prefix` |
| Section 3, "Credential" row | "static, supplied from the environment" | add "kept in the Postgres component's env file (`infrastructure/components/postgresql/.env`); Vault is not involved" |
| Section 3, last paragraph and section 5 | "Kafka Connect or Debezium Server is decided in the plan" | "Kafka Connect with the Debezium PostgreSQL connector (spec 013 research R4)" |
| Top note | "Option names … should be checked" | add "checked against Debezium 3.7 by spec 013" |

## Spec 012: `specs/012-profile-next-rebuild/spec.md`

| Where | Now | Change to |
|---|---|---|
| Assumptions, "Each service has its own database inside one shared database instance, with logical decoding enabled once for that instance." | single shared instance | "The shared instance is kept for now with logical decoding enabled on it (spec 013); each service is expected to get its own instance later" |
| Assumptions, relay bullet | "the plan chooses between Kafka Connect and Debezium Server" | "Kafka Connect (spec 013)" |
| `checklists/requirements.md` line about the relay decision | open | note that spec 013 closes it |

## Spec 003: `specs/003-infrastructure-adapters/`

| File | Where | Change |
|---|---|---|
| `spec.md` | FR-024 | Keep the list; remove "outbox" from the banned items, and add "Amended by spec 013: writing an account-deleted announcement row is allowed; broker clients, Debezium, CDC engine code and Kafka routing remain banned" |
| `spec.md` | SC-008 | Reword to "zero references to messaging **clients**, change-capture engine code or secrets-vault credential rotation" with the same note |
| `contracts/shared-components.md` | table row "Not set: `wal_level=logical`, replication slots, per-service init users" | Replace with "`wal_level=logical` is set (spec 013). Replication slots and per-service init users are still not set by the compose file; the relay role and publication come from `scripts/create-relay-role.sh`" |
| `tasks.md` | T077 "Do NOT set `wal_level=logical`…" | Add note: superseded by spec 013 |
| `research.md` | "Not set: `wal_level=logical` and replication slots" | Same note |
| `plan.md` | any line saying outbox or CDC is excluded | Same note where present |

## Docs

| File | Change |
|---|---|
| `docs/plans/identityaccess-infrastructure-migration.md` | Line "Do not migrate `messaging`, `cache`, `persistence`, `cdc`, `exception`, or `scheduler` packages." stays true (no legacy code is migrated); add one sentence pointing to spec 013 for the new outbox |
| `infrastructure/components/postgresql/README.md` | Already notes `wal_level=logical`; add the role script and the "re-run after the service migrates" note, and change the first line from "One shared PostgreSQL 17 instance for the whole project" only if the instance model changes (not in this feature) |
| `infrastructure/components/kafka/README.md` | Add the Connect service, `scripts/` list and the relay runbook link |
| `infrastructure/AGENTS.md` | No change |

## Test sources

| File | Change |
|---|---|
| `ForbiddenDependencyTest.java` | See [publisher-port.md](publisher-port.md) section 5 |
| `StructureRules.java` | New `PORT_MAP` row |
| `PostgreSqlTestSupport.java` | Container started with `-c wal_level=logical` |
