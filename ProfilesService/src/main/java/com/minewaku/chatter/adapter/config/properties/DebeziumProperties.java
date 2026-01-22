package com.minewaku.chatter.adapter.config.properties;

import java.util.Properties;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.adapter.db.postgresql.VariableServerRepository;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
public class DebeziumProperties {

    private Properties props = new Properties();

    public DebeziumProperties(
            VaultDebeziumProperties vaultDebeziumProperties,
            VariableServerRepository variableServerRepository) {

        // offset storage config
        props.setProperty("name", "engine");
        props.setProperty("offset.storage", "org.apache.kafka.connect.storage.KafkaOffsetBackingStore");
		props.setProperty("offset.storage.group.id", "debezium-offset-reader-group.profiles");
		props.setProperty("offset.storage.bootstrap.servers", "localhost:5003");
		props.setProperty("offset.storage.client.id", "debezium-offset-client-" + variableServerRepository.getServerId());
        props.setProperty("offset.storage.topic", "debezium-offsets.profiles");
        props.setProperty("offset.storage.partitions", "3");
        props.setProperty("offset.storage.replication.factor", "1");
        
        // kafka bootstrap servers
        props.setProperty("bootstrap.servers", "localhost:5003"); 
        props.setProperty("offset.flush.interval.ms", "60000");


        // db config
        props.setProperty("connector.class", "io.debezium.connector.postgresql.PostgresConnector");
        props.setProperty("database.hostname", "localhost");
        props.setProperty("database.port", "5441");
        props.setProperty("database.user", vaultDebeziumProperties.getUsername());
        props.setProperty("database.password", vaultDebeziumProperties.getPassword());
        props.setProperty("database.dbname", "chatter");
        props.setProperty("database.server.name", "profiles-postgresql-chatter");
        props.setProperty("database.server.id", String.valueOf(variableServerRepository.getServerId()));
        
        // Plugin 
        props.setProperty("plugin.name", "pgoutput");
        
        // Prefix topic (doesn't need but required)
        props.setProperty("topic.prefix", "dummy-topic-prefix");

        // schema history config
        props.setProperty("schema.history.internal", "io.debezium.storage.kafka.history.KafkaSchemaHistory");
        props.setProperty("schema.history.internal.kafka.topic", "debezium-schema-history.profiles");
        props.setProperty("schema.history.internal.kafka.bootstrap.servers", "localhost:5003");

        // filter config (only detect from insert queries)
        props.setProperty("transforms", "filter");       
        props.setProperty("transforms.filter.type", "io.debezium.transforms.Filter");
        props.setProperty("transforms.filter.language", "jsr223.groovy");
        props.setProperty("transforms.filter.condition", "value.op == 'c'");

        // Only capture outbox table
        props.setProperty("table.include.list", "public.outbox");
        props.setProperty("include.schema.changes", "false");

        // performance tuning
        props.setProperty("max.queue.size", "4096"); 
        props.setProperty("max.batch.size", "1024"); 
    }
}

