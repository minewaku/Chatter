package com.minewaku.chatter.message.application.messaging.subcriber.integration;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.message.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.message.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.AttachmentFileStorageUploadedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.subcriber.core.IntegrationEventBatchSubscriber;
import com.minewaku.chatter.message.domain.model.message.repository.MessageRepository;
import com.minewaku.chatter.message.domain.sharedkernel.service.UniqueStringIdGenerator;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AttachmentFileStorageUploadedIntegrationEventBatchSubscriber implements IntegrationEventBatchSubscriber<AttachmentFileStorageUploadedIntegrationEvent> {
    
    private final MessageRepository messageRepository;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final IntegrationEventPublisher integrationEventPublisher;

    public AttachmentFileStorageUploadedIntegrationEventBatchSubscriber(
        MessageRepository messageRepository,
        UniqueStringIdGenerator uniqueStringIdGenerator,
        OutboxStore outboxStore
    ) {
        this.messageRepository = messageRepository;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    @Override
    @Transactional
    public void handle(List<AttachmentFileStorageUploadedIntegrationEvent> events) {
        events.stream().forEach(event -> {
            log.info("Test log for AttachmentFileStorageUploadedIntegrationEvent: {}", event);
        });
    }
}