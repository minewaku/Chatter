# Cloudflare Tunnel Infrastructure

Runs `cloudflared` so remote clients can access the local Docker network (Kafka UI, APIs, etc.) through a Cloudflare Zero Trust tunnel.

## Quick Start

```bash
cp .env.example .env     # set CLOUDFLARE_TUNNEL_TOKEN (Zero Trust > Networks > Tunnels); .env is gitignored
docker compose -p cloudflared_chatter -f docker-compose.yml up -d
```

See the [bootstrap runbook](docs/runbooks/bootstrap.md) for full details (token retrieval, dependency order, teardown).

## Notes
- The compose file expects the shared `chatter-net` network to exist (created automatically when other stacks run).
- `kafka-ui-chatter` is listed as a dependency because the tunnel typically exposes that UI. Adjust `depends_on` as you add new services.
