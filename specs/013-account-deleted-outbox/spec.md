# Feature Specification: Account-Deleted Outbox and Relay (identity-access)

**Feature Branch**: `profile-next`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Account-deleted outbox for identity-access and its change-capture relay. Implements the identity-access side of spec 012 (FR-019 to FR-025, FR-028 and `contracts/account-deleted-event.md`), which is the producer of the event that `profile-next` consumes." Covers three areas: the `identity-access` service code, the guardrails (architecture tests and earlier specs) that currently forbid it, and the repo-level infrastructure that runs the relay.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Deleting an account always leaves exactly one announcement (Priority: P1)

When an account is soft-deleted in `identity-access`, the service writes an account-deleted announcement in the same transaction as the deletion. If the deletion commits, the announcement exists; if it rolls back, it does not. A delete that changes nothing (the account was already deleted) writes nothing. The announcement is removed again in the same transaction, so the table is empty at rest and nothing needs cleaning up later.

**Why this priority**: Everything downstream (the relay, the `profile-next` consumer, privacy cleanup of profile data) depends on this single guarantee: no deletion without its announcement, no announcement without its deletion.

**Independent Test**: Run the soft-delete use case against a real database with no broker and no relay present. Commit a deletion and check the announcement was written and removed within the same transaction, using the database's change log or a transaction-scoped probe. Roll one back and check nothing was written. Delete the same account again and check nothing was written.

**Acceptance Scenarios**:

1. **Given** an active account, **When** the owner deletes it and the transaction commits, **Then** one announcement carrying an event id, the user id and the time of deletion was written in that transaction.
2. **Given** a deletion that rolls back for any reason, **When** the transaction ends, **Then** no announcement exists and none can be seen by the relay.
3. **Given** an account that is already deleted, **When** the delete is requested again and changes nothing, **Then** no announcement is written.
4. **Given** the message broker and the relay are both stopped, **When** an account is deleted, **Then** the deletion succeeds at normal speed and is unaffected.
5. **Given** any number of completed deletions, **When** the announcements table is inspected at rest, **Then** it holds zero rows.
6. **Given** a caller that tries to record an announcement outside the deletion transaction, **When** it does so, **Then** the call fails and writes nothing.

---

### User Story 2 - A standalone relay publishes each announcement to the broker (Priority: P1)

A change-capture relay, running as its own process outside `identity-access`, reads the announcement from the database's change log and publishes it to the message broker. It publishes each announcement at least once, keyed by user id, with a flat JSON value and the event id and event type as headers. After a crash or restart it resumes from its last recorded position and catches up on everything written meanwhile.

**Why this priority**: Without the relay the announcement never leaves the database, so `profile-next` never learns about the deletion. It is as essential as Story 1, but it can only be verified once Story 1 exists.

**Independent Test**: With Story 1 in place, delete an account and read the broker topic: exactly one message with the right key, headers and value. Stop the relay, delete several accounts, restart it, and check every deletion appears. Stop the broker, delete an account, start the broker, and check the message arrives.

**Acceptance Scenarios**:

1. **Given** a committed announcement, **When** the relay processes it, **Then** one message appears on the user topic with key = user id, headers carrying the event id and the event type, and value = a flat JSON object with event id, user id and time of deletion and no personal data.
2. **Given** the relay was stopped while deletions happened, **When** it starts again, **Then** it resumes from its last position and publishes every announcement written meanwhile.
3. **Given** the relay crashed after publishing but before recording its position, **When** it restarts, **Then** the announcement may be published a second time. This is accepted; consumers tolerate it.
4. **Given** the broker is unavailable, **When** announcements are written, **Then** they are published once the broker is back, and none is lost.
5. **Given** the announcement row is removed in the same transaction that wrote it, **When** the relay reads the change log, **Then** it publishes one message per announcement and nothing for the removal.
6. **Given** no announcements arrive for a long time, **When** the relay sits idle, **Then** its position keeps advancing so the database does not retain change log for it.
7. **Given** a new event type or a new field is added later, **When** it is published to the same topic, **Then** existing consumers are not broken (additive evolution, see the contract).

---

### User Story 3 - The service stays free of messaging code, and the tests prove it (Priority: P2)

A maintainer can trust the boundary: `identity-access` contains the code that records an announcement, and nothing that talks to a broker or runs a relay. The architecture tests that today forbid anything called "outbox" are changed so recording an announcement is allowed, while everything that must stay out still fails the build.

**Why this priority**: The boundary is what makes the design maintainable. It is P2 because the feature works without the tests, but the tests are what stop it eroding.

**Independent Test**: Run the architecture tests. They pass with the new port and adapter present. Add a throwaway class that depends on a Kafka or Debezium type, or a class named after them, and confirm the build fails.

**Acceptance Scenarios**:

1. **Given** the new announcement port and its adapter, **When** the architecture tests run, **Then** they pass.
2. **Given** a class that uses a Kafka or Debezium type, or has "Kafka", "Debezium" or "Cdc" in its name, **When** the architecture tests run, **Then** the build fails.
3. **Given** a build file that declares a Kafka or Debezium dependency, **When** the dependency check runs, **Then** it fails.
4. **Given** the core module, **When** it is inspected, **Then** it knows the announcement only as a business-language outbound port, with no knowledge of tables, JSON, brokers or the relay.
5. **Given** the new port, **When** the port-to-adapter check runs, **Then** it finds exactly one adapter at the documented location.

---

### User Story 4 - A developer can bring up the whole path from a clean checkout (Priority: P2)

A developer follows the documented steps to enable logical decoding on the shared database, create the relay's database role and publication, start the relay, register its connector, and create the topics, then deletes an account and sees the message on the topic. Every step can be repeated safely and is documented, including how to reset it.

**Why this priority**: The relay, role, publication and topics are new platform pieces. If they cannot be brought up repeatably, Stories 1 and 2 cannot be verified by anyone but their author.

**Independent Test**: On a machine with no relay state, follow the runbook from the start and finish with a message on the user topic for a freshly deleted account.

**Acceptance Scenarios**:

1. **Given** the shared database, **When** it is started with the new settings, **Then** logical decoding is on. The documentation says changing this needs a restart and keeps existing data.
2. **Given** the service has migrated at least once, **When** the role-and-publication script is run, **Then** it creates the dedicated relay role and a publication containing only the announcements table, and running it again changes nothing and fails nothing.
3. **Given** the service has not yet created the announcements table, **When** the script is run, **Then** it stops with a message to start `identity-access` once first, and changes nothing.
4. **Given** the topic script, **When** it is run, **Then** the user topic and the `profile-next` dead-letter topic exist, and the old topics are not created.
5. **Given** the connector definition, **When** the registration script is run with credentials from the environment, **Then** the relay starts capturing, and running it again updates it without creating a duplicate.
6. **Given** the runbook, **When** a developer needs to start from scratch, **Then** it states how to remove the relay, its replication slot and its data.

---

### Edge Cases

- Two delete requests for the same account arrive at the same moment: the account's own concurrency control lets one change it, so only one announcement is written.
- The deletion succeeds but a later step in the same transaction fails: everything rolls back, including the announcement.
- The relay is down for a long time: the database keeps change log for its slot, and the documentation says how to watch for that and what to do (drop the slot, accept the loss, or restore the relay).
- The relay role's credentials are missing, or still placeholder values such as `change-me…`, in the Postgres component's env file: the scripts fail with a clear message and change nothing.
- The announcements table does not exist yet when the relay starts: the relay does not crash the broker or the database; the runbook gives the right order.
- The topic does not exist when the relay first publishes: documented as a prerequisite, with a check in the runbook.
- The event id in the message must be stable across repeats of the same announcement: the id is written once with the row, never regenerated by the relay.
- The row's removal appears in the change log as a delete: it must not become a message, and must not produce an empty "tombstone" record.
- The payload carries only identifiers and times: no email, username or birthday ever reaches the broker.
- Postgres still runs without logical decoding after a restart because the setting was not applied: the role script and runbook include a check for it.

## Requirements *(mandatory)*

### Functional Requirements

**Announcement in `identity-access` (service code)**

- **FR-001**: When `identity-access` soft-deletes an account and the account actually changes to deleted, it MUST record an account-deleted announcement in the same transaction as the deletion.
- **FR-002**: A delete that changes nothing (the account is already deleted) MUST NOT record an announcement.
- **FR-003**: If the deletion's transaction rolls back, no announcement MAY remain or reach the relay.
- **FR-004**: The announcement MUST be removed in the same transaction that wrote it, so the announcements record is empty at rest. No scheduled cleanup MAY be needed or added.
- **FR-005**: An announcement MUST carry a unique, time-ordered event id; the user id; the time of deletion, taken from the account's own deletion time and not from a new clock read; the kind of record it concerns (`user`); and the event type (`UserSoftDeleted`). It MUST carry no personal data.
- **FR-006**: Account operations MUST NOT depend on broker or relay availability. `identity-access` MUST hold no broker client and contain no relay code.
- **FR-007**: Recording an announcement MUST be impossible outside an existing transaction: a call made without one MUST fail and write nothing.
- **FR-008**: The core module MUST learn of the announcement only through one outbound port named `UserAccountEventPublisher`, with a single event-named method for deletion that takes the user id and the time of deletion. The port MUST be in business language and MUST NOT mention storage, message format, broker or relay.
- **FR-009**: The announcements record MUST be created by the service's own migrations, inside the service's own schema (`identity_access`), and MUST contain the fields of the contract: id, kind, aggregate id, event type, payload and time of occurrence.

**Guardrails (architecture tests and earlier specs)**

- **FR-010**: Spec 003 FR-024 and SC-008, and the architecture test that forbids classes named "Outbox", MUST be amended so writing the announcement is allowed. The class-name ban on "Outbox" is the only ban removed.
- **FR-011**: The bans on Kafka, Debezium and CDC types and class names, the pom checks for Kafka and Debezium dependencies, the ban on secret-store credential rotation, and the ban on the legacy request filters MUST remain and keep failing the build.
- **FR-012**: The new port MUST be registered in the port-to-adapter map, with exactly one adapter at its documented location, so the existing port-adapter check passes.

**Relay (standalone process)**

- **FR-013**: A change-capture relay running as a standalone process MUST publish each announcement to the message broker at least once, keyed by user id, with a flat JSON value (event id, user id, time of deletion), and with the event id and event type as headers. The topic MUST be chosen by the kind of record, `identityaccess.user.v1`, not by event type. The environment prefix is deployment configuration.
- **FR-014**: The relay MUST resume from its last recorded position after a restart and MUST NOT lose an announcement across a relay or broker outage.
- **FR-015**: The relay MUST publish one message per announcement. The removal of the row MUST NOT produce a message, including an empty one.
- **FR-016**: The relay MUST use its own database credential, limited to replication and to reading the announcements record, MUST capture no other table (the publication it reads from MUST contain only the announcements table), and MUST receive that credential as a static value from the environment. The credential is not managed or rotated by the secret store.
- **FR-017**: While no announcements arrive, the relay MUST keep advancing its position (about every 10 seconds) so the database does not retain change log for it.
- **FR-018**: The relay's broker producer MUST wait for full acknowledgement and MUST be idempotent. Delivery is at least once.
- **FR-019**: The connector definition MUST live with the other shared infrastructure, in the Kafka component, MUST name the real table (`identity_access.outbox`), MUST contain no credentials, and MUST be registered by a repeatable script.

**Platform (shared infrastructure)**

- **FR-020**: The shared database instance MUST run with logical decoding on, and the documentation MUST state that applying it needs a restart that keeps data.
- **FR-021**: A script run manually against the live database MUST create the relay's role and a publication that contains only the announcements table, MUST read the role's credentials from the Postgres component's own env file and refuse empty or placeholder values, MUST be safe to run again, and MUST stop with a clear message, changing nothing, if the announcements table does not exist yet (the service has to have migrated once). It is not part of the database's first-start initialisation, because the data folder already exists.
- **FR-022**: The topic script MUST create `identityaccess.user.v1` (3 partitions) and `identityaccess.user.v1.profile-next.dlt`, and MUST no longer create the legacy event and outbox topics or the per-service offset and schema-history topics for the old design.
- **FR-023**: The relay MUST have a documented runbook covering start, verification, reset, and how to watch for retained change log when the relay is down.

**Amendments to earlier work**

- **FR-024**: The contract (`specs/012-profile-next-rebuild/contracts/account-deleted-event.md`) MUST be corrected: the table is in the `identity_access` schema, and the message value is expanded to flat JSON instead of an escaped string. Spec 012's assumption about one shared instance with logical decoding enabled once MUST be reworded: the shared instance is kept for now, with logical decoding enabled on it, and each service is expected to get its own instance later. The spec 003 contract and task that forbid logical decoding on the shared database MUST be amended. `docs/plans/identityaccess-infrastructure-migration.md` MUST be updated where it contradicts this feature.

### Key Entities *(include if feature involves data)*

- **Account-deleted announcement**: written with a soft-deletion and removed in the same transaction. Holds event id, kind of record (`user`), user id, event type (`UserSoftDeleted`), payload, and the time of occurrence. It exists only for the duration of one transaction.
- **Account-deleted message**: what consumers receive. Key = user id; value = flat JSON of event id, user id, time of deletion; headers = event id, event type; topic = `identityaccess.user.v1`.
- **Relay position**: how far the relay has read the database's change log. Survives restarts and is advanced while idle.
- **Relay credential**: a dedicated database login limited to replication and to reading the announcements record. Static, supplied from the environment.
- **Publication**: the database-side filter that limits what the relay can see. It contains only the announcements table, so no other table's changes (account rows, password hashes) ever reach the relay.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Over a run of 1,000 deletion-style transactions (each writes and removes one announcement exactly as the service does, about 10% rolled back) with random broker and relay outages, 0 committed announcements are lost, 0 rolled-back ones are published, and every committed one produces at least one message. Service-side atomicity itself is covered by the User Story 1 tests.
- **SC-002**: With the broker and the relay both stopped, 100% of account deletions succeed, and their latency shows no measurable increase over deleting before this feature existed.
- **SC-003**: Under normal operation, a committed deletion reaches the broker within 30 seconds, and within 5 minutes after a broker or relay outage ends.
- **SC-004**: After every completed deletion in the test suite, the announcements record holds 0 rows.
- **SC-005**: 100 repeated delete requests for the same account produce exactly 1 announcement.
- **SC-006**: 100% of published messages have a value that parses as a flat JSON object with exactly the event id, user id and time of deletion, a key equal to the user id, and both headers. 0 messages contain an email, username or birthday.
- **SC-007**: `identity-access` has 0 dependencies on a broker client or a relay library, and the architecture tests (including the retained Kafka, Debezium and CDC bans, and the port-to-adapter check for the new port) pass on every build.
- **SC-008**: During a one-hour idle test, the change log retained for the relay does not grow.
- **SC-009**: A developer who has never run the relay can follow the runbook from a clean checkout and see a message for a freshly deleted account in under 30 minutes, and can repeat every script without errors.

## Assumptions

- This feature is the `identity-access` side of spec 012 (User Story 5; FR-019 to FR-025; FR-028). The `profile-next` consumer, the dead-letter handling and the access-revoked event for the `message` service stay in spec 012 or later specs and are out of scope here.
- Spec 012's contract `contracts/account-deleted-event.md` is the source for message shape, topic, headers and evolution rules. This feature corrects it where it is wrong (FR-024) and does not redesign it.
- The relay runs as Kafka Connect with the Debezium PostgreSQL connector, chosen over Debezium Server because the Kafka component already exists and Connect registers connectors over a REST interface. The connector setting that expands the payload to flat JSON, and any other option names, are to be checked against the Debezium version actually deployed.
- The service writes the announcement with plain parameterised SQL through `JdbcClient`, not Spring Data JDBC entities and mappers: there is no core model to map, a Spring Data JDBC `save` with an application-assigned id would try an update, and the payload is a JSON column. The adapter is `JdbcUserAccountEventPublisher`, placed under `adapter/outbound/user` in the same capability and technology layout as the existing adapters. It requires an existing transaction.
- The payload is a small record serialised with the Jackson already on the classpath. The event id comes from the existing UUIDv7 generator and is converted to the column type only if that is required.
- The relay credential values `IDENTITYACCESS_DEBEZIUM_USERNAME` and `IDENTITYACCESS_DEBEZIUM_PASSWORD` live only in the Postgres component's env file (`infrastructure/components/postgresql/.env`, with names in `.env.example`). Vault serves only the Java services, so these two keys are removed from the Vault component's example env file. They are plain static values for the relay, and placeholder values are refused.
- The publication is `FOR TABLE identity_access.outbox`, so it can only be created after the service has migrated once. This was chosen over a schema-wide publication so that no other table's rows (password hashes and salts) stream to the relay. The connector's table list repeats the restriction.
- The shared PostgreSQL component is kept, with logical decoding enabled on it. It was already set in `docker-compose.yml` and `README.md`; the container still needs a restart. Per-service instances are expected later and are out of scope.
- Existing Kafka topic names with the `dev.*` prefix and the `debezium-offsets` and `debezium-schema-history` topics belong to the old design and may be dropped from the script; nothing in the new design reads them. The Postgres connector does not use schema-history topics.
- The announcements table is named `outbox` (the event router's convention) and lives in `identity_access`. Java classes use the name of the port (`UserAccountEventPublisher` and its adapter), not "Outbox".
- Flyway migrations in this repo are partly stale. The new migration is written against what the module actually runs (schema `identity_access`) and does not depend on older migration docs.
- Removing the `SUPERUSER` grant from Vault's dynamic role template is a separate cleanup and out of scope.
- Legacy `services/profile` and legacy messaging code are not modified or reused.
- An empty `services/profile-next` service skeleton (same multi-module layout and architecture-test setup as `identity-access`, no behaviour, no outbox, no publishing, no consumer) is created alongside this feature, as its own final phase of the task list, because spec 012 needs a place to land its consumer. It adds no requirement to this spec.
