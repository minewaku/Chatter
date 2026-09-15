#!/usr/bin/env bash
set -euo pipefail

BROKER="kafka-chatter:5003"
REPLICATION_FACTOR=1

create_topic() {
  local topic="$1"
  local partitions="${2:-3}"
  shift 2 || true

  kafka-topics \
    --create \
    --bootstrap-server "$BROKER" \
    --replication-factor "$REPLICATION_FACTOR" \
    --partitions "$partitions" \
    --topic "$topic" \
    --if-not-exists \
    "$@"
}

# Debezium offsets
create_topic "debezium-offsets.identityaccess" 3 --config cleanup.policy=compact
create_topic "debezium-offsets.profile" 3 --config cleanup.policy=compact
create_topic "debezium-offsets.message" 3 --config cleanup.policy=compact

# Debezium schema history
create_topic "debezium-schema-history.identityaccess" 1 --config retention.ms=-1
create_topic "debezium-schema-history.profile"        1 --config retention.ms=-1
create_topic "debezium-schema-history.message"        1 --config retention.ms=-1

# Identity Access
create_topic "dev.shared.event.identityaccess.user" 3
create_topic "dev.internal.event.identityaccess.outbox" 3
create_topic "dev.internal.event.identityaccess.dlq" 3

# Profile
create_topic "dev.internal.event.profile.outbox" 3
create_topic "dev.internal.event.profile.dlq" 3
create_topic "dev.private.event.profile.file.avatarFileStorageUploaded" 3
create_topic "dev.private.event.profile.file.bannerFileStorageUploaded" 3
create_topic "dev.private.event.socket.profile" 3

# Message
create_topic "dev.internal.event.message.outbox" 3
create_topic "dev.internal.event.message.dlq" 3
create_topic "dev.private.event.message.file.guildIconFileStorageUploaded" 3
create_topic "dev.private.event.message.file.attachmentFileStorageUploaded" 3
create_topic "dev.private.event.socket.message.guild" 3
create_topic "dev.private.event.socket.message.channel" 3