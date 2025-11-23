package com.minewaku.chatter.adapter.messaging.publisher;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.adapter.db.mysql.JpaOutboxRepository;

@Configuration
public class OutboxIntegrationFlow {
	
	String clientId = "authentication-service";

	@Bean
	IntegrationFlow outboxFlow(
		KafkaTemplate<String, String> kafkaTemplate,
	    ObjectMapper objectMapper,
	    JpaOutboxRepository outboxRepository) {

	    return IntegrationFlow.from("outboxChannel")
	            .enrichHeaders(h -> {
	            	h.headerFunction("event_type", m -> { 
	            		try { 
	            			JsonNode root = objectMapper.readTree((String) m.getPayload()); 
	            			return root.path("after").path("eventType").asText(); 
	            		} catch (Exception e) { 
	            			throw new RuntimeException(e); 
	            		} 
	            	}); 
	            	
	            	h.headerFunction("kafka_topic", m -> mapEventToTopic((String) m.getHeaders().get("event_type"))); 
	            	h.headerFunction("client_id", m -> mapEventToClientId((String) m.getHeaders().get("event_type")));
	            })
	            .handle((payload, headers) -> {
	            	try { 
		            	JsonNode root = objectMapper.readTree((String) payload);
		            	Long outboxId = root.path("after").path("id").asLong();
		            	String headerTopic = (String) headers.get("kafka_topic");
		            	kafkaTemplate.send(headerTopic, (String) payload)
		                            .thenRun(() -> outboxRepository.markProcessed(outboxId));
	
		                return null;
	            	} catch (Exception e) { 
            			throw new RuntimeException(e); 
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

