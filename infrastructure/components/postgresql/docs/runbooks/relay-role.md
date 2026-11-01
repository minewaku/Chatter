# Relay role and publication

`scripts/create-relay-role.sh` prepares the database side of the identity-access CDC relay. See
[../../../kafka/docs/runbooks/relay.md](../../../kafka/docs/runbooks/relay.md) for the whole path.

## What it does

- Creates the login role named by `IDENTITYACCESS_DEBEZIUM_USERNAME` with `LOGIN REPLICATION` (never superuser) and the
  password from `IDENTITYACCESS_DEBEZIUM_PASSWORD`, or updates the password if the role exists.
- Grants it `CONNECT` on the database, `USAGE` on schema `identity_access` and `SELECT` on `identity_access.outbox`.
- Creates the publication `identity_access_outbox` containing only `identity_access.outbox` (or resets it to that).
- Safe to run again; a second run changes nothing. It never prints the password.

## Preconditions (the script stops and changes nothing otherwise)

- The container `postgresql-chatter` is running with `wal_level=logical`.
- `identity_access.outbox` exists, i.e. `identity-access` has started once and run its migrations.
- Both credentials are set and are not `change-me…` placeholders.

## Where the credentials come from

`infrastructure/components/postgresql/.env` (gitignored; copy the names from `.env.example`), or the same variables in
your shell. Vault is not involved: it serves only the Java services.

## Run

```bash
bash infrastructure/components/postgresql/scripts/create-relay-role.sh
```

## Undo

```bash
docker exec postgresql-chatter psql -U admin -d chatter -c "DROP PUBLICATION IF EXISTS identity_access_outbox"
docker exec postgresql-chatter psql -U admin -d chatter -c "SELECT pg_drop_replication_slot('identity_access_outbox')"  # if present
docker exec postgresql-chatter psql -U admin -d chatter -c "REVOKE ALL ON identity_access.outbox FROM <relay user>; DROP OWNED BY <relay user>; DROP ROLE <relay user>"
```
