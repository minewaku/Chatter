# Contract: Publisher Port and Adapter

Requirements: spec FR-001 to FR-009, FR-010 to FR-012.

## 1. Core port

```java
package com.minewaku.chatter.identityaccess.user.internal.port;

public interface UserAccountEventPublisher {

    /** Announces that the account was soft-deleted. Must run inside the deletion's transaction. */
    void publishDeleted(UserId userId, Instant deletedAt);
}
```

Rules: business language only; no table, JSON, broker or relay vocabulary; no framework imports (the core architecture rule already enforces this).

## 2. Use-case change

`SoftDeleteUserAccountService` gains the port as a constructor argument and calls it once, inside `if (changed)`, after `sessionRevocation.revokeAllFor(...)`:

```java
publisher.publishDeleted(account.id(), account.deletedAt());
```

No other use case changes. The handler stays single-method.

## 3. Adapter

| Item | Value |
|---|---|
| Class | `com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.event.jdbc.JdbcUserAccountEventPublisher` (public, `@Component`) |
| Collaborators | `JdbcClient`, `UniqueStringIdGenerator`, the Boot Jackson `ObjectMapper` |
| Method annotation | `@Transactional(propagation = Propagation.MANDATORY)` |
| Statements | `INSERT INTO outbox (id, aggregate_type, aggregate_id, event_type, payload, occurred_at) VALUES (?, 'user', ?, 'UserSoftDeleted', ?::jsonb, ?)` then `DELETE FROM outbox WHERE id = ?`, both with bound parameters |
| Payload type | package-private record `UserDeletedMessage(String eventId, String userId, Instant occurredAt)` |
| Failure | No catch: any exception rolls back the deletion |

The table name is unqualified because the datasource's `currentSchema` is `identity_access`, the same assumption the existing repositories make.

## 4. Port-adapter map

Add to `StructureRules.PORT_MAP`:

```java
row("user.internal.port.UserAccountEventPublisher", "JdbcUserAccountEventPublisher", "user.event.jdbc")
```

`PortAdapterMapTest` then enforces exactly one implementation at that package and name, and fails if the port is ever left unmapped.

## 5. Architecture tests

| Test | Change |
|---|---|
| `ForbiddenDependencyTest.noClassesNamedAfterDroppedFeatures` | Remove `haveSimpleNameContaining("Outbox")`. Keep `Debezium`, `Cdc`, `Kafka`, `Vault` and the legacy filter names |
| `ForbiddenDependencyTest.noMessagingVaultOrJjwtTypes` | No change: broker, Debezium, Vault and jjwt packages stay banned |
| `ForbiddenDependencyTest.theModulePomDeclaresNoDroppedDependencies` | No change: `kafka`, `debezium`, `vault`, `jjwt` stay banned |
| Class comment | Reword from "nothing from the old service's messaging, CDC, outbox…" to say writing the announcement row is allowed and broker, relay and CDC code are not |
| `PersistenceShapesArePackagePrivate` | No change. `UserDeletedMessage` ends in `Message`, so the rule does not apply, but it is package-private anyway |

## 6. Tests required

| Test | Proves |
|---|---|
| Core unit test, fake publisher | Called once with the account's id and `deletedAt` when the delete changes the account; not called when already deleted; not called when the password check fails |
| Adapter integration test, Postgres with `wal_level=logical` and a `test_decoding` slot | Committed call yields exactly one `INSERT` and one `DELETE` for `outbox`, and the table is empty afterwards; a call followed by a thrown exception (rollback) yields nothing; a call with no transaction throws and writes nothing; the stored payload parses to exactly `eventId`, `userId`, `occurredAt` |
| End-to-end test through the real use case | One API deletion yields one `INSERT` and one `DELETE` and an empty table; two concurrent delete requests for one account yield exactly one `INSERT`. The "100 repeated deletes yield one announcement" check (SC-005) is the core unit test |
| Port-adapter map and forbidden-dependency tests | Pass with the new port; fail for a throwaway class using a Kafka type |
