# Redis (shared)

One shared Redis 7 instance for the whole project. Services share it by key prefix; identity-access writes keys under `identity-access:`.

- Image: `redis:7`
- Container: `redis-chatter`
- Host port: `6379` (override with `REDIS_PORT`)
- Network: `chatter-net`
- Credentials: `REDIS_USERNAME` / `REDIS_PASSWORD` in `.env` (gitignored; copy `.env.example`), passed to Redis on the command line, not stored in `redis.conf`. The `default` user also requires the password (it cannot be disabled: Redis replays the append-only file as `default`, and a disabled user loses every transaction on restart)
- Persistence: append-only file in `./data`

## Start

Run from this folder, after `cp .env.example .env` and filling it in:

```bash
docker compose -p redis_chatter -f docker-compose.yml up -d
```

## Connect

```bash
redis-cli --user "$REDIS_USERNAME" --pass "$REDIS_PASSWORD" --no-auth-warning
```

## Stop

```bash
docker compose -p redis_chatter -f docker-compose.yml down
```

Every service shares this account: copy it into the `<SERVICE>_REDIS_USERNAME` / `_PASSWORD` entries of [`../vault/.env`](../vault/README.md).

To wipe data, see [docs/runbooks/reset.md](docs/runbooks/reset.md).
