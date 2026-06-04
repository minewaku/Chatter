package com.minewaku.chatter.profile.application.service.command.profile;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.profile.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.BannerFileStorageUploadedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.profile.application.port.inbound.command.file.usecase.HandleUploadNotificationUseCase;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.profile.domain.sharedkernel.service.UniqueStringIdGenerator;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service("handleUploadBannerNotificationUseCase")
public class HandleUploadBannerNotificationApplicationService implements HandleUploadNotificationUseCase {

    private final AssetStorage assetStorage;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final IntegrationEventPublisher integrationEventPublisher;

    public HandleUploadBannerNotificationApplicationService (
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
        log.info("handling upload banner notification for fileHash {}", command.headers().get("fileHash"));

        AssetStorage.UploadResult result = assetStorage.handleUploadNotification(command.headers(), command.body());

        String eventId = uniqueStringIdGenerator.generate();
		BannerFileStorageUploadedIntegrationEvent event = new BannerFileStorageUploadedIntegrationEvent(
            result.namespace().toString(),
            result.context(),
            result.fileHash(),
            result.contentType(),
            result.fileName(),
            result.fileSize()
        );

        IntegrationEventWrapper<BannerFileStorageUploadedIntegrationEvent> wrapper = new IntegrationEventWrapper<>(eventId, null, event);
		integrationEventPublisher.publish(wrapper);
        return null;
    }
}
