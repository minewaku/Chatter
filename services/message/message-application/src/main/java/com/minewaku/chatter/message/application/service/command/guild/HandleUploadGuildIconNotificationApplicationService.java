package com.minewaku.chatter.message.application.service.command.guild;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.message.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.GuildIconFileStorageUploadedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.message.application.port.inbound.command.asset.HandleUploadNotificationUseCase;
import com.minewaku.chatter.message.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.message.domain.sharedkernel.service.UniqueStringIdGenerator;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("handleUploadGuildIconNotificationUseCase")
public class HandleUploadGuildIconNotificationApplicationService implements HandleUploadNotificationUseCase {

    private final AssetStorage assetStorage;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final IntegrationEventPublisher integrationEventPublisher;

    public HandleUploadGuildIconNotificationApplicationService (
            AssetStorage assetStorage,
            UniqueStringIdGenerator uniqueStringIdGenerator,
            OutboxStore outboxStore) {
            
        this.assetStorage = assetStorage;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    @Override
    @Retry(name = "transientDataAccess")
    public Void handle(Command command) {
        log.info("handling upload avatar notification for fileHash {}", command.headers().get("fileHash"));

        AssetStorage.UploadResult result = assetStorage.handleUploadNotification(command.headers(), command.body());

        String eventId = uniqueStringIdGenerator.generate();
		GuildIconFileStorageUploadedIntegrationEvent event = new GuildIconFileStorageUploadedIntegrationEvent(
            result.namespace().toString(),
            result.context(),
            result.fileHash(),
            result.contentType(),
            result.fileSize(),
            result.fileName()
        );

        IntegrationEventWrapper<GuildIconFileStorageUploadedIntegrationEvent> wrapper = new IntegrationEventWrapper<>(eventId, "", event);
		integrationEventPublisher.publish(wrapper);
        return null;
    }
}
