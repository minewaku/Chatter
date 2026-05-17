package com.minewaku.chatter.message.application.messaging.subcriber.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.message.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.message.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.AssetCreatedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.FileStorageUploadedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.message.application.messaging.subcriber.core.IntegrationEventBatchSubscriber;
import com.minewaku.chatter.message.domain.model.asset.model.Asset;
import com.minewaku.chatter.message.domain.model.asset.model.AssetDimension;
import com.minewaku.chatter.message.domain.model.asset.model.AssetId;
import com.minewaku.chatter.message.domain.model.asset.model.Namespace;
import com.minewaku.chatter.message.domain.model.asset.repository.AssetRepository;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.message.model.Message;
import com.minewaku.chatter.message.domain.model.message.model.MessageId;
import com.minewaku.chatter.message.domain.model.message.repository.MessageRepository;
import com.minewaku.chatter.message.domain.sharedkernel.service.TimeBasedIdGenerator;
import com.minewaku.chatter.message.domain.sharedkernel.service.UniqueStringIdGenerator;

@Service
public class FileStorageUploadedIntegrationEventsBatchSubscriber implements IntegrationEventBatchSubscriber<FileStorageUploadedIntegrationEvent> {

    private final AssetRepository assetRepository;
    private final MessageRepository messageRepository;
    private final TimeBasedIdGenerator timeBasedIdGenerator;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final IntegrationEventPublisher integrationEventPublisher;

    public FileStorageUploadedIntegrationEventsBatchSubscriber(
            AssetRepository assetRepository,
            MessageRepository messageRepository,
            TimeBasedIdGenerator timeBasedIdGenerator,
            UniqueStringIdGenerator uniqueStringIdGenerator,
            OutboxStore outboxStore) {

        this.assetRepository = assetRepository;
        this.messageRepository = messageRepository;
        this.timeBasedIdGenerator = timeBasedIdGenerator;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    @Override
    @Transactional
    public void handle(List<FileStorageUploadedIntegrationEvent> events) {
        if (events == null || events.isEmpty()) return;

        Map<ChannelId, List<FileStorageUploadedIntegrationEvent>> eventsByChannel = events.stream()
            .filter(e -> e.getContext().containsKey("channelId") && e.getContext().containsKey("messageId"))
            .collect(Collectors.groupingBy(
                e -> new ChannelId(Long.parseLong(e.getContext().get("channelId").toString()))
            ));

        Set<Message> messagesToUpdate = new HashSet<>();
        List<IntegrationEventWrapper<?>> integrationEventsToPublish = new ArrayList<>();

        // 1. Fetch toàn bộ Messages liên quan
        Map<MessageId, Message> messageMap = fetchMessages(eventsByChannel);

        // 2. Thu thập toàn bộ fileHashes MỚI từ các Event để fetch Asset theo batch
        // Do dùng List và thao tác Add, ta không cần fetch các hash cũ lên để trừ refCount ở event này.
        Set<String> allHashesToFetch = events.stream()
            .map(FileStorageUploadedIntegrationEvent::getFileHash)
            .collect(Collectors.toSet());

        // 3. Fetch toàn bộ Assets liên quan lên bộ nhớ (Cache)
        Map<String, Asset> assetCache = StreamSupport.stream(
                assetRepository.findAllByFileHashIn(allHashesToFetch).spliterator(), false)
            .collect(Collectors.toMap(Asset::getFileHash, Function.identity()));

        // 4. Xử lý logic cập nhật refCount
        for (Map.Entry<ChannelId, List<FileStorageUploadedIntegrationEvent>> entry : eventsByChannel.entrySet()) {
            ChannelId channelId = entry.getKey();
            
            for (FileStorageUploadedIntegrationEvent event : entry.getValue()) {
                MessageId messageId = new MessageId(Long.parseLong(event.getContext().get("messageId").toString()));
                Message message = messageMap.get(messageId);
                
                if (message == null) continue;

                String newHash = event.getFileHash();

                // --- Xử lý Asset (Tăng refCount hoặc Tạo mới) ---
                // Hàm addAssetHash trả về true nếu hash chưa tồn tại trong danh sách của message và đã được add thành công.
                // Việc này giúp xử lý idempotent: Nếu cùng 1 event bị trùng lặp, ta không cộng dồn refCount sai.
                if (message.addAssetHash(newHash)) { 
                    Asset newAsset = assetCache.get(newHash);
                    
                    if (newAsset != null) {
                        newAsset.increaseRefCount(); // Tăng refCount
                    } else {
                        // Tạo mới nếu Asset chưa từng tồn tại
                        AssetId assetId = new AssetId(timeBasedIdGenerator.generate());
                        newAsset = new Asset(
                                assetId,
                                Namespace.valueOf(event.getNamespace()),
                                newHash,
                                new AssetDimension(event.getWidth(), event.getHeight()),
                                event.getFileSize(),
                                1, // Khởi tạo refCount = 1
                                null
                        );
                        assetCache.put(newHash, newAsset); // Lưu ngược vào cache để dùng cho các event trùng hash phía sau

                        // Publish AssetCreatedIntegrationEvent (Chỉ bắn đi khi Asset thực sự được tạo mới)
                        AssetCreatedIntegrationEvent assetCreatedEvent = new AssetCreatedIntegrationEvent(
                            newAsset.getId().getValue(),
                            newAsset.getNamespace().name(),
                            newAsset.getFileHash(),
                            Map.of("channelId", channelId.getValue(), "messageId", messageId.getValue())
                        );
                        integrationEventsToPublish.add(new IntegrationEventWrapper<>(
                            uniqueStringIdGenerator.generate(), null, assetCreatedEvent
                        ));
                    }

                    // Thêm message vào danh sách chờ lưu
                    messagesToUpdate.add(message);
                }
            }
        }

        // 5. Lưu toàn bộ dữ liệu ở cuối hàm (Batch Update/Insert) để tối ưu I/O
        assetRepository.saveAll(assetCache.values());
        messageRepository.saveAll(messagesToUpdate); 
        integrationEventPublisher.publish(integrationEventsToPublish);
    }

    // Hàm helper để tách logic lấy messages theo batch
    private Map<MessageId, Message> fetchMessages(Map<ChannelId, List<FileStorageUploadedIntegrationEvent>> eventsByChannel) {
        Map<MessageId, Message> messageMap = new HashMap<>();
        for (Map.Entry<ChannelId, List<FileStorageUploadedIntegrationEvent>> entry : eventsByChannel.entrySet()) {
            Set<MessageId> messageIds = entry.getValue().stream()
                .map(e -> new MessageId(Long.parseLong(e.getContext().get("messageId").toString())))
                .collect(Collectors.toSet());
            
            StreamSupport.stream(
                messageRepository.findAllByIdInChannel(entry.getKey(), messageIds).spliterator(), false)
                .forEach(msg -> messageMap.put(msg.getMessageId(), msg));
        }
        return messageMap;
    }
}