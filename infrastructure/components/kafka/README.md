# Kafka Infrastructure

Local Kafka + Zookeeper stack for Chatter, plus a Kafka Connect worker with the Debezium PostgreSQL connector that relays
the identity-access account-deleted announcement (needs `IDENTITYACCESS_DEBEZIUM_*` in the shell; see the relay runbook).

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
* [Identity-access CDC relay](docs/runbooks/relay.md)

## Key Files

```text
docker-compose.yml                 # includes kafka-connect-chatter (Debezium relay, REST on 127.0.0.1:5004)
scripts/create-topics.sh
scripts/register-connectors.sh     # registers connectors/*.json with Kafka Connect
scripts/soak-relay.sh              # outage soak for the relay
connectors/identity-access-outbox.json
docs/runbooks/
docs/references/
```
