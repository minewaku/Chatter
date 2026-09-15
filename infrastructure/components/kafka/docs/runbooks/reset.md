# Kafka Reset

Recreate the local Kafka cluster from a clean state.

> Run from the repository root.

## Stop

```bash
docker compose \
  -p kafka_chatter \
  --project-directory infrastructure/components/kafka \
  -f infrastructure/components/kafka/docker-compose.yml \
  down
```

## Delete State

```powershell
Remove-Item -Recurse -Force `
  infrastructure/components/kafka/.data_kafka, `
  infrastructure/components/kafka/.data_zookeeper
```

## Restart

```bash
docker compose \
  -p kafka_chatter \
  --project-directory infrastructure/components/kafka \
  -f infrastructure/components/kafka/docker-compose.yml \
  up -d
```

## Provision Topics

```bash
bash infrastructure/components/kafka/scripts/create-topics.sh
```
