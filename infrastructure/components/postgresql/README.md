# PostgreSQL (shared)

One shared PostgreSQL 17 instance for the whole project. Each service owns a schema inside the `chatter` database; identity-access uses `identity_access`.

- Image: `postgres:17`
- Container: `postgresql-chatter`
- Host port: `5432` (override with `POSTGRES_PORT`)
- Network: `chatter-net`
- Logical decoding: `wal_level=logical`, needed by the identity-access outbox relay. Changing it needs a container restart (`down` then `up -d`; data is kept).
- Credentials: `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` in `.env` (gitignored; copy `.env.example`)

## Start

Run from this folder, after `cp .env.example .env` and filling it in:

```bash
docker compose -p postgresql_chatter -f docker-compose.yml up -d
```

## Connect

```bash
psql "postgresql://$POSTGRES_USER:$POSTGRES_PASSWORD@localhost:5432/chatter"
```

## Stop

```bash
docker compose -p postgresql_chatter -f docker-compose.yml down
```

Vault uses this instance for dynamic credentials of every service: copy the admin user and password into the `<SERVICE>_DB_VAULT_USERNAME` / `_PASSWORD` entries of [`../vault/.env`](../vault/README.md).

Data is kept in `./data`. To wipe it, see [docs/runbooks/reset.md](docs/runbooks/reset.md).

## Relay role and publication

The identity-access outbox relay (Debezium) needs a dedicated login role and a publication. Put real values for
`IDENTITYACCESS_DEBEZIUM_USERNAME` and `IDENTITYACCESS_DEBEZIUM_PASSWORD` in `.env`, start identity-access once so its
migrations create `identity_access.outbox`, then run:

```bash
bash infrastructure/components/postgresql/scripts/create-relay-role.sh
```

Details: [docs/runbooks/relay-role.md](docs/runbooks/relay-role.md). The full relay path is in
[../kafka/docs/runbooks/relay.md](../kafka/docs/runbooks/relay.md).
