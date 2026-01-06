package com.minewaku.chatter.adapter.messaging.publisher;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class OutboxIntegrationFlow {
	
	String clientId = "authentication-service";

	@Bean
	IntegrationFlow outboxFlow(
			KafkaTemplate<String, String> kafkaTemplate,
			ObjectMapper objectMapper
	) {

		return IntegrationFlow.from("outboxChannel")
				.enrichHeaders(h -> {
					h.headerFunction("event_type", m -> {
						try {
							JsonNode root = objectMapper.readTree((String) m.getPayload());
							return root.path("after").path("eventType").asText();
						} catch (Exception e) {
							throw new RuntimeException("Failed to extract event_type", e);
						}
					});

					h.headerFunction("kafka_topic", m -> mapEventToTopic((String) m.getHeaders().get("event_type")));
					h.headerFunction("client_id", m -> mapEventToClientId((String) m.getHeaders().get("event_type")));
				})
				.handle((payload, headers) -> {
					try {
						String headerTopic = (String) headers.get("kafka_topic");
						String payloadString = (String) payload;
						kafkaTemplate.send(headerTopic, payloadString).get();
						
						return null;
					} catch (Exception e) {
						throw new RuntimeException("Failed to send to Kafka", e);
					}
				})
				.get();
	}

    private String mapEventToTopic(String eventType) {
        if (eventType.contains("USER_CREATED")) {
            return "dev.shared.entity.authentication.user.id";
        } else if(eventType.contains("USER_LOCKED") || 
        		eventType.contains("USER_UNLOCKED") ||
        		eventType.contains("USER_SOFT_DELETED") ||
        		eventType.contains("USER_RESTORED") ||
        		eventType.contains("USER_HARD_DELETED")) {
			return "dev.shared.command.authentication.accessibility.id";
		}
        
        throw new IllegalArgumentException("Unknown event type : " + eventType);
    }
    
    private String mapEventToClientId(String eventType) {
		if (eventType.contains("USER_CREATED")) {
			return "dev.com.minewaku.chatter.authentication.authentication-service.user";
		} else if(eventType.contains("USER_LOCKED") ||
				eventType.contains("USER_UNLOCKED") ||
				eventType.contains("USER_SOFT_DELETED") ||
				eventType.contains("USER_RESTORED") ||
				eventType.contains("USER_HARD_DELETED")) {
			return "dev.com.minewaku.chatter.authentication.authentication-service.accessibility";
		}
		
		throw new IllegalArgumentException("Unknown event type " + eventType);
	}
}

