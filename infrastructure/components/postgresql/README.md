# PostgreSQL (shared)

One shared PostgreSQL 17 instance for the whole project. Each service owns a schema inside the `chatter` database; identity-access uses `identity_access`.

- Image: `postgres:17`
- Container: `postgresql-chatter`
- Host port: `5432` (override with `POSTGRES_PORT`)
- Network: `chatter-net`
- Credentials (development only): `admin` / `123456`, database `chatter`

## Start

Run from this folder:

```bash
docker compose -p postgresql_chatter -f docker-compose.yml up -d
```

## Connect

```bash
psql "postgresql://admin:123456@localhost:5432/chatter"
```

## Stop

```bash
docker compose -p postgresql_chatter -f docker-compose.yml down
```

Data is kept in `./data`. To wipe it, see [docs/runbooks/reset.md](docs/runbooks/reset.md).
