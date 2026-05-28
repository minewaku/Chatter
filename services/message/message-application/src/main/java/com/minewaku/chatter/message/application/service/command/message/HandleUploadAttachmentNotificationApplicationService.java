package com.minewaku.chatter.message.application.service.command.message;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.message.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.AttachmentFileStorageUploadedIntegrationEvent;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.message.application.port.inbound.command.message.HandleUploadAttachmentNotificationUseCase;
import com.minewaku.chatter.message.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.message.domain.sharedkernel.service.UniqueStringIdGenerator;


@Service
public class HandleUploadAttachmentNotificationApplicationService implements HandleUploadAttachmentNotificationUseCase {

    private final AssetStorage assetStorage;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final IntegrationEventPublisher integrationEventPublisher;

    public HandleUploadAttachmentNotificationApplicationService (
            AssetStorage assetStorage,
            UniqueStringIdGenerator uniqueStringIdGenerator,
            OutboxStore outboxStore) {
            
        this.assetStorage = assetStorage;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    @Override
    //push all notifications from file storage webhook to message bus in order to batch process them later instead of handle every notification one by one
    //gonna cost some extra outbox events but at least improves database performance (i think)
    public Void handle(Command command) {

        AssetStorage.UploadResult result = assetStorage.handleUploadNotification(command.headers(), command.body());

        String eventId = uniqueStringIdGenerator.generate();
		AttachmentFileStorageUploadedIntegrationEvent event = new AttachmentFileStorageUploadedIntegrationEvent(
            result.namespace().toString(),
            result.context(),
            result.fileHash(),
            result.width(),
            result.height(),
            result.fileSize()
        );

        IntegrationEventWrapper<AttachmentFileStorageUploadedIntegrationEvent> wrapper = new IntegrationEventWrapper<>(eventId, "", event);
		integrationEventPublisher.publish(wrapper);
        return null;
    }
}
