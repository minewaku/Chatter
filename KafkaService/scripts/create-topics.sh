#!/bin/bash

kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 3 --topic dev.shared.entity.authentication.user.id
kafka-topics --create --bootstrap-server kafka-chatter:5003 --replication-factor 1 --partitions 3 --topic dev.shared.command.authentication.accessibility.id