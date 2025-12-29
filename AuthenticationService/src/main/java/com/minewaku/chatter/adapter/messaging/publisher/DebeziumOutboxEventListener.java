package com.minewaku.chatter.adapter.messaging.publisher;

import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.adapter.config.properties.DebeziumProperties;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import jakarta.annotation.PostConstruct;

@Component
public class DebeziumOutboxEventListener {

	private final OutboxEventPublisher publisher;
	private final DebeziumProperties debeziumProperties;

	public DebeziumOutboxEventListener(
			OutboxEventPublisher publisher,
			DebeziumProperties debeziumProperties) {
		this.publisher = publisher;
		this.debeziumProperties = debeziumProperties;
	}

	@PostConstruct
	public void start() {
		DebeziumEngine<ChangeEvent<String, String>> engine = DebeziumEngine.create(Json.class)
				.using(debeziumProperties.getProps())
				.notifying(record -> publisher.publish(record.value()))
				.build();

		Executors.newSingleThreadExecutor().submit(engine);
	}
}
