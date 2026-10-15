# Contract: Shared Database Components

**Feature**: [spec.md](../spec.md)

Two new components under `infrastructure/components`, laid out like `kafka` and `vault`. Existing components and other services' compose files are not modified.

## PostgreSQL — `infrastructure/components/postgresql`

```text
postgresql/
├── docker-compose.yml
├── README.md                    # start, stop, connect, reset
├── .gitignore                   # data/
└── docs/runbooks/reset.md       # wipe data and start clean
```

| Item | Value |
|---|---|
| Compose service / container | `postgresql-chatter` |
| Image | `postgres:17` |
| Network | `chatter-net` (declared with `name: chatter-net`, same as other components) |
| Host port | `${POSTGRES_PORT:-5432}` → container `5432` |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` | `admin` / `123456` / `chatter` |
| Data | `./data:/var/lib/postgresql/data` |
| Health check | `pg_isready -U admin -d chatter` |
| Restart policy | `unless-stopped` |
| Not set | `wal_level=logical`, replication slots, per-service init users |

Each service creates and owns its own schema through its migrations (identity-access: `identity_access`).

Start: `docker compose -p postgresql_chatter -f docker-compose.yml up -d` (run inside the component folder, like the vault README).
Connect: `psql "postgresql://admin:123456@localhost:5432/chatter"`.

## Redis — `infrastructure/components/redis`

```text
redis/
├── docker-compose.yml
├── config/redis.conf            # ACL user, append-only persistence
├── README.md
├── .gitignore                   # data/
└── docs/runbooks/reset.md
```

| Item | Value |
|---|---|
| Compose service / container | `redis-chatter` |
| Image | `redis:7` |
| Network | `chatter-net` |
| Host port | `${REDIS_PORT:-6379}` → container `6379` |
| ACL | `user default on >123456 ~* &* +@all` (password required; must stay enabled so AOF replay of transactions works); `user admin on >123456 ~* &* +@all` |
| Persistence | `appendonly yes`; data in `./data:/data` |
| Health check | `redis-cli --user admin --pass 123456 --no-auth-warning ping` returns `PONG` |
| Restart policy | `unless-stopped` |

Services share the instance by key prefix (identity-access: `identity-access:`).
Start: `docker compose -p redis_chatter -f docker-compose.yml up -d`.
Connect: `redis-cli --user admin --pass 123456`.

## Routing documents

`infrastructure/AGENTS.md` gains two lines under **Components**: `components/postgresql/` and `components/redis/` (pointing to their README files, since the Kafka component also has no local `AGENTS.md`).

## Acceptance

1. A clean checkout starts each stack with one documented command and the `admin` / `123456` login succeeds.
2. Stopping and restarting keeps the data (account row; Redis keys written before restart).
3. `docker ps` shows no port conflict with the existing components when Kafka, Vault, and both new stacks run together.
