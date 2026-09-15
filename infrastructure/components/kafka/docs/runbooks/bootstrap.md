# Kafka Bootstrap

Initialize the local Kafka stack.

> Run from the repository root.

## Prepare Data Directories

On Windows, create the bind-mount directories when missing:

```powershell
New-Item -ItemType Directory -Force `
  infrastructure/components/kafka/.data_kafka, `
  infrastructure/components/kafka/.data_zookeeper

icacls "infrastructure/components/kafka/.data_kafka" /grant Everyone:"(OI)(CI)F" /T
icacls "infrastructure/components/kafka/.data_zookeeper" /grant Everyone:"(OI)(CI)F" /T
```

## Start

```bash
docker compose \
  -p kafka_chatter \
  --project-directory infrastructure/components/kafka \
  -f infrastructure/components/kafka/docker-compose.yml \
  up -d
```

## Verify

```bash
docker compose \
  -p kafka_chatter \
  --project-directory infrastructure/components/kafka \
  -f infrastructure/components/kafka/docker-compose.yml \
  ps
```

## Provision Topics

```bash
bash infrastructure/components/kafka/scripts/create-topics.sh
```

## Stop

```bash
docker compose \
  -p kafka_chatter \
  --project-directory infrastructure/components/kafka \
  -f infrastructure/components/kafka/docker-compose.yml \
  down
```

For a clean rebuild, see [reset.md](reset.md).
