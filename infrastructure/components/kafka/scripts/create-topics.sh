#!/usr/bin/env bash
set -euo pipefail

# kafka-topics only exists inside the broker container; when run from the host, hand over to it.
if ! command -v kafka-topics >/dev/null 2>&1; then
  exec docker exec -i kafka-chatter bash /opt/kafka/scripts/create-topics.sh
fi

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

# Identity Access: account-deleted events (one topic per kind of record, see specs/013-account-deleted-outbox)
create_topic "identityaccess.user.v1" 3
create_topic "identityaccess.user.v1.profile-next.dlt" 3
# written by the CDC relay's heartbeat so its replication slot keeps advancing while no announcements arrive
create_topic "__debezium-heartbeat.identityaccess" 1 --config cleanup.policy=delete --config retention.ms=3600000

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