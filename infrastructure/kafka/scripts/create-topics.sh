#!/bin/bash

# debezium offsets
kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 3 --topic debezium-offsets.identityaccess --config cleanup.policy=compact --if-not-exists
kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 3 --topic debezium-offsets.profile --config cleanup.policy=compact --if-not-exists
kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 3 --topic debezium-offsets.message --config cleanup.policy=compact --if-not-exists

# debezium schema history
kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 1 --topic debezium-schema-history.identityaccess --config retention.ms=-1 --if-not-exists
kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 1 --topic debezium-schema-history.profile --config retention.ms=-1 --if-not-exists
kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 1 --topic debezium-schema-history.message --config retention.ms=-1 --if-not-exists

# domain & internal events
kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 3 --topic dev.shared.event.identityaccess.user --if-not-exists
kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 3 --topic dev.internal.event.identityaccess.outbox --if-not-exists
kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 3 --topic dev.internal.event.identityaccess.dlq --if-not-exists

kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 3 --topic dev.internal.event.profile.outbox --if-not-exists
kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 3 --topic dev.internal.event.profile.dlq --if-not-exists
kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 3 --topic dev.private.event.profile.file.avatarFileStorageUploaded --if-not-exists
kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 3 --topic dev.private.event.profile.file.bannerFileStorageUploaded --if-not-exists

kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 3 --topic dev.internal.event.message.outbox --if-not-exists
kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 3 --topic dev.internal.event.message.dlq --if-not-exists