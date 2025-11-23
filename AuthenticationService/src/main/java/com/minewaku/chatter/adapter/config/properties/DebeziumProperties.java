package com.minewaku.chatter.adapter.config.properties;

import java.util.Properties;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import com.minewaku.chatter.adapter.db.mysql.VariableServerRepository;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
@RefreshScope
public class DebeziumProperties {

    private Properties props = new Properties();

    public DebeziumProperties(
            DataSourceProperties dataSourceProperties,
            VariableServerRepository variableServerRepository) {
                
		props.setProperty("name", "engine");
		// Common settings
		props.setProperty("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore");
		props.setProperty("offset.storage.file.filename", "/path/to/storage/offsets.dat");
		props.setProperty("offset.flush.interval.ms", "60000");

		// PostgreSQL connector (active)
		props.setProperty("connector.class", "io.debezium.connector.postgresql.PostgresConnector");
		props.setProperty("database.port", "5440");
		props.setProperty("database.dbname", "chatter");
		props.setProperty("database.server.name", "authentication-postgresql-chatter");
		
        // logical decoding plugin (pgoutput is preferred for modern Postgres)
		props.setProperty("plugin.name", "pgoutput");
		
		/* begin connector properties */
		props.setProperty("database.hostname", "localhost");
		props.setProperty("database.user", dataSourceProperties.getUsername());
		props.setProperty("database.password", dataSourceProperties.getPassword());
		props.setProperty("database.server.id", String.valueOf(variableServerRepository.getServerId()));
		props.setProperty("topic.prefix", "dummy-topic-goes-brrr-hehe"); // not used but required
		props.setProperty("schema.history.internal", "io.debezium.storage.file.history.FileSchemaHistory");
		props.setProperty("schema.history.internal.file.filename", "/path/to/storage/schemahistory.dat");
		
		// Add transforms to filter only create events (op == 'c')
		props.setProperty("transforms.filter.type", "io.debezium.transforms.Filter");
		props.setProperty("transforms.filter.language", "jsr223.groovy");
		props.setProperty("transforms.filter.condition", "value.op == 'c'");


		props.setProperty("table.include.list", "chatter.outbox");
		props.setProperty("include.schema.changes", "false");

		props.setProperty("max.queue.size", "4096");        // giới hạn hàng đợi nội bộ
		props.setProperty("max.batch.size", "1024");        // giới hạn batch
    }
}

