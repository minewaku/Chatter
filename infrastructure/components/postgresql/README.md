# PostgreSQL (shared)

One shared PostgreSQL 17 instance for the whole project. Each service owns a schema inside the `chatter` database; identity-access uses `identity_access`.

- Image: `postgres:17`
- Container: `postgresql-chatter`
- Host port: `5432` (override with `POSTGRES_PORT`)
- Network: `chatter-net`
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
