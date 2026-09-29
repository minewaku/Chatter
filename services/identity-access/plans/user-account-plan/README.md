# User Account rebuild handoff

This package lets a new AI session rebuild Identity Access without prior chat context.

## Scope and locations

- Legacy/reference service: `D:\Project\Java\2026\chatter\services\identity-access`
- Replacement target: sibling `D:\Project\Java\2026\chatter\services\identity-access-next`

Verify that the target path exists and is the intended replacement project before writing. Do not edit the legacy service; it is reference material only. If the target is missing or unexpected, stop and ask for direction.

This is a clean-start replacement. Do not migrate legacy production accounts, passwords, sessions, or tokens.

## Required reading before implementation

Read these legacy sources before beginning the matching work:

1. `details-structure-plan.txt` for architecture, module direction, persistence trade-offs, and feature boundaries.
2. `features.txt` for feature inventory and validation expectations.
3. The user model under `identityaccess-domain/src/main/java/.../aggregate/user/model/`, especially `User`, value objects, enablement, and credentials.
4. The account schema: `identityaccess-start/src/main/resources/db/migration/V1__init_schema.sql`.
5. The registration use case: `identityaccess-application/src/main/java/.../command/user/RegisterUserApplicationService.java`, plus related registration service/tests as needed.

These are reference, not a design to copy. The state model and event policy in this package supersede the legacy design.

## Non-negotiable decisions

- Clean start: no legacy-data migration.
- No domain events or integration outbox in the first phase.
- Use a direct Spring Data JDBC model in core; do not add a persistence-only row/entity mapper just for framework purity.
- Make replacement changes only in `identity-access-next`; do not edit this legacy service.
- Preserve numeric `BIGINT` user IDs through the selected Snowflake strategy.

Read [domain order](01-domain-order.md), [User Account model](02-user-account-model.md), and [implementation/test plan](03-implementation-and-test-plan.md) in order before coding.
