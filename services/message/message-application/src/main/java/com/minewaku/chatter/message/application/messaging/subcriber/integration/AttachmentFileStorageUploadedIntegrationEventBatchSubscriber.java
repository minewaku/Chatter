package com.minewaku.chatter.message.application.messaging.subcriber.integration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.message.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.message.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.AssetAttachedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.AttachmentCreatedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.AttachmentFileStorageUploadedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.message.application.messaging.subcriber.core.IntegrationEventBatchSubscriber;
import com.minewaku.chatter.message.application.port.outbound.repository.ProcessedEventRepository;
import com.minewaku.chatter.message.application.port.outbound.repository.ProcessedEventRepository.ProcessedEventRecord;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.message.event.AttachmentAddedDomainEvent;
import com.minewaku.chatter.message.domain.model.message.model.Message;
import com.minewaku.chatter.message.domain.model.message.model.MessageId;
import com.minewaku.chatter.message.domain.model.message.repository.MessageRepository;
import com.minewaku.chatter.message.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.message.domain.sharedkernel.service.UniqueStringIdGenerator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AttachmentFileStorageUploadedIntegrationEventBatchSubscriber implements IntegrationEventBatchSubscriber<AttachmentFileStorageUploadedIntegrationEvent> {
    
    private final MessageRepository messageRepository;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final ProcessedEventRepository processedEventRepository;
    private final IntegrationEventPublisher integrationEventPublisher;

    public AttachmentFileStorageUploadedIntegrationEventBatchSubscriber(
        MessageRepository messageRepository,
        UniqueStringIdGenerator uniqueStringIdGenerator,
        ProcessedEventRepository processedEventRepository,
        OutboxStore outboxStore
    ) {
        this.messageRepository = messageRepository;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.processedEventRepository = processedEventRepository;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore); 
    }

    @Override
    @Transactional
    public void handle(List<AttachmentFileStorageUploadedIntegrationEvent> events) {
        if (events == null || events.isEmpty()) return;

        Set<String> eventIds = events.stream()
                .map(AttachmentFileStorageUploadedIntegrationEvent::getEventId)
                .collect(Collectors.toSet());

        Set<String> alreadyProcessed = processedEventRepository.findAllExistingIds(eventIds);
        List<AttachmentFileStorageUploadedIntegrationEvent> newEvents = events.stream()
                .filter(e -> !alreadyProcessed.contains(e.getEventId()))
                .toList();

        if (newEvents.isEmpty()) {
            log.debug("All {} events in batch already processed, skip", events.size());
            return;
        }

        Map<String, Map<String, List<AttachmentFileStorageUploadedIntegrationEvent>>> groupedEvents = new HashMap<>();

        for (AttachmentFileStorageUploadedIntegrationEvent event : events) {
            Map<String, Object> context = event.getContext();
            String channelIdStr = (String) context.get("channelId");
            String messageIdStr = (String) context.get("messageId");

            if (channelIdStr == null || messageIdStr == null) {
                log.warn("Skip event. FileHash: {}", event.getFileHash());
                continue;
            }

            groupedEvents
                .computeIfAbsent(channelIdStr, k -> new HashMap<>())
                .computeIfAbsent(messageIdStr, k -> new ArrayList<>())
                .add(event);
        }

        Set<Message> messagesToSave = new HashSet<>();
        List<IntegrationEventWrapper<?>> eventsToPublish = new ArrayList<>();

        for (var channelEntry : groupedEvents.entrySet()) {
            ChannelId channelId = new ChannelId(Long.parseLong(channelEntry.getKey()));
            Map<String, List<AttachmentFileStorageUploadedIntegrationEvent>> messagesInChannel = channelEntry.getValue();

            Set<MessageId> messageIds = messagesInChannel.keySet().stream()
                    .map(id -> new MessageId(Long.parseLong(id)))
                    .collect(Collectors.toSet());

            Map<MessageId, Message> messageMap = messageRepository
                    .findAllByIdInChannel(channelId, messageIds)
                    .stream()
                    .collect(Collectors.toMap(Message::getId, msg -> msg));

            for (var messageEntry : messagesInChannel.entrySet()) {
                MessageId messageId = new MessageId(Long.parseLong(messageEntry.getKey()));
                Message message = messageMap.get(messageId);

                if (message == null) {
                    log.warn("Cant find message id {} in channel {}. skip.", messageId, channelId);
                    continue;
                }

                // 2. Apply mutations
                for (AttachmentFileStorageUploadedIntegrationEvent event : messageEntry.getValue()) {
                    String fileName = event.getFileName();
                    Long fileSize = event.getFileSize() != null ? event.getFileSize().longValue() : 0L;

                    message.addAttachment(
                            event.getFileHash(),
                            fileName,
                            event.getContentType(),
                            fileSize
                    );
                }

                List<AttachmentAddedDomainEvent> attachmentAddedEvents =
                        attachmentAddedDomainEventsFiltered(message.getDomainEvents());

                if (attachmentAddedEvents.isEmpty()) continue;

                messagesToSave.add(message);

                for (AttachmentAddedDomainEvent domainEvent : attachmentAddedEvents) {
                    eventsToPublish.add(new IntegrationEventWrapper<>(
                            uniqueStringIdGenerator.generate(),
                            domainEvent.getFileHash(),
                            new AssetAttachedIntegrationEvent(
                                    domainEvent.getNamespace(),
                                    domainEvent.getFileHash(),
                                    domainEvent.getContentType(),
                                    domainEvent.getFileName(),
                                    domainEvent.getFileSize().intValue()
                            )
                    ));

                    eventsToPublish.add(new IntegrationEventWrapper<>(
                            uniqueStringIdGenerator.generate(),
                            domainEvent.getFileHash(),
                            new AttachmentCreatedIntegrationEvent(
                                    domainEvent.getMessageId().toString(),
                                    domainEvent.getChannelId().toString(),
                                    domainEvent.getNamespace(),
                                    domainEvent.getFileHash(),
                                    domainEvent.getContentType(),
                                    domainEvent.getFileName(),
                                    domainEvent.getFileSize().intValue()
                            )
                    ));
                }
            }
        }

        if (!messagesToSave.isEmpty()) {
            messageRepository.saveAll(messagesToSave);
        }

        if (!eventsToPublish.isEmpty()) {
            integrationEventPublisher.publish(eventsToPublish);
        }

        List<ProcessedEventRecord> processedRecords = newEvents.stream()
                .map(e -> new ProcessedEventRecord(e.getEventId(), Instant.now()))
                .toList();

        processedEventRepository.saveAll(processedRecords);
    }

    private List<AttachmentAddedDomainEvent> attachmentAddedDomainEventsFiltered(
            List<DomainEvent> domainEvents) {
        return domainEvents.stream()
                .filter(e -> e instanceof AttachmentAddedDomainEvent)
                .map(e -> (AttachmentAddedDomainEvent) e)
                .toList();
    }
}