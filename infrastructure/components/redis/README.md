# Redis (shared)

One shared Redis 7 instance for the whole project. Services share it by key prefix; identity-access writes keys under `identity-access:`.

- Image: `redis:7`
- Container: `redis-chatter`
- Host port: `6379` (override with `REDIS_PORT`)
- Network: `chatter-net`
- Credentials (development only): ACL user `admin` / `123456`; the `default` user also requires the password (it cannot be disabled: Redis replays the append-only file as `default`, and a disabled user loses every transaction on restart)
- Persistence: append-only file in `./data`

## Start

Run from this folder:

```bash
docker compose -p redis_chatter -f docker-compose.yml up -d
```

## Connect

```bash
redis-cli --user admin --pass 123456 --no-auth-warning
```

## Stop

```bash
docker compose -p redis_chatter -f docker-compose.yml down
```

To wipe data, see [docs/runbooks/reset.md](docs/runbooks/reset.md).
