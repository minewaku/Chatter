# Kafka Infrastructure

Local Kafka + Zookeeper stack for Chatter.

## Quick Start

```bash
docker compose -p kafka_chatter up -d
```

For full setup, see [Bootstrap](docs/runbooks/bootstrap.md).

## Source of Truth

`scripts/create-topics.sh` defines all managed topics and their configuration.

Do not maintain a separate topic catalog.

## Operations

* [Bootstrap](docs/runbooks/bootstrap.md)
* [Manage topics](docs/runbooks/topics.md)
* [Reset local state](docs/runbooks/reset.md)

## Key Files

```text
docker-compose.yml
assets/topics.json
scripts/create-topics.sh
docs/runbooks/
docs/references/
```
