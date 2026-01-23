#!/bin/bash

#debezium offsets
kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 3 --topic debezium-offsets.authentication --config cleanup.policy=compact
kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 3 --topic debezium-offsets.profiles --config cleanup.policy=compact

#debezium schema history
kafka-topics --create --bootstrap-server localhost:5003 --replication-factor 1 --partitions 1 --topic debezium-schema-history.authentication --config retention.ms=-1
kafka-topics --create --bootstrap-server localhost:5003 --replication-factor 1 --partitions 1 --topic debezium-schema-history.profiles --config retention.ms=-1

kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 3 --topic dev.shared.entity.authentication.user.id
kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 3 --topic dev.shared.command.authentication.accessibility.id