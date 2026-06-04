package com.minewaku.chatter.message.presentation.messaging.consumer;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class SingleKafkaConsumer {

    // private final ObjectMapper objectMapper;
    // private final AssetCreatedIntegrationEventSubcriber assetCreatedIntegrationEventSubcriber;


    // @KafkaListener(
    //     topics = "dev.internal.event.message.outbox", 
    //     groupId = "dev-com.minewaku.message.chatter.outbox",
    //     containerFactory = "singleFactory"
    // )
    // public void consumeAssetEvents(
    //         @Payload String payload, 
    //         @Header(value = "eventType", required = false) String eventType) {
            
    //     try {
    //         if (eventType == null) {
    //             return;
    //         }

    //         switch (eventType) {
    //             case "AssetCreated": {
    //                 AssetCreatedIntegrationEvent eventData = 
    //                         objectMapper.readValue(payload, AssetCreatedIntegrationEvent.class);
    //                 assetCreatedIntegrationEventSubcriber.handle(eventData);
    //                 break;
    //             }
    //             default:
    //                 log.debug("default skip: {}", eventType);
    //         }

    //     } catch (Exception e) {
    //         log.error("Kafka error: {}", payload, e);
    //         throw new RuntimeException(e);
    //     }
    // }

}
