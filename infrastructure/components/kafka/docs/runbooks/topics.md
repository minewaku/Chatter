# Kafka Topics

Manage the local Kafka topic inventory.

## Source of Truth

`infrastructure/components/kafka/scripts/create-topics.sh` defines all managed topics and their configuration.

Do not maintain a separate topic catalog.

## Provision

```bash
bash infrastructure/components/kafka/scripts/create-topics.sh
```

The script is idempotent and can be rerun after topic changes.

## Add or Modify

Edit `infrastructure/components/kafka/scripts/create-topics.sh`, then rerun it.

> Existing topic configuration may require explicit alteration when it cannot be changed through topic creation.

## Delete

```bash
kafka-topics \
  --bootstrap-server kafka-chatter:5003 \
  --delete \
  --topic <topic_name>
```
