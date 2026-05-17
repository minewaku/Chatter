package com.minewaku.chatter.profile.application.service.command.file;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.profile.application.exception.EntityNotFoundException;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.FileStorageUploadedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.profile.application.port.inbound.command.file.usecase.HandleUploadNotificationUseCase;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.profile.domain.model.file.model.Asset;
import com.minewaku.chatter.profile.domain.model.file.model.AssetDimension;
import com.minewaku.chatter.profile.domain.model.file.model.AssetId;
import com.minewaku.chatter.profile.domain.model.file.model.Namespace;
import com.minewaku.chatter.profile.domain.model.file.repository.AssetRepository;
import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;
import com.minewaku.chatter.profile.domain.sharedkernel.service.TimeBasedIdGenerator;
import com.minewaku.chatter.profile.domain.sharedkernel.service.UniqueStringIdGenerator;

@Service
public class HandleUploadNotificationApplicationService implements HandleUploadNotificationUseCase {

    private final AssetRepository assetRepository;
    private final ProfileRepository profileRepository;
    private final AssetStorage assetStorage;
    private final TimeBasedIdGenerator  timeBasedIdGenerator;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final IntegrationEventPublisher integrationEventPublisher;

    public HandleUploadNotificationApplicationService (
            AssetRepository assetRepository,
            ProfileRepository profileRepository,
            AssetStorage assetStorage,
            TimeBasedIdGenerator timeBasedIdGenerator,
            UniqueStringIdGenerator uniqueStringIdGenerator,
            OutboxStore outboxStore) {
            
        this.assetRepository = assetRepository;
        this.profileRepository = profileRepository;
        this.assetStorage = assetStorage;
        this.timeBasedIdGenerator = timeBasedIdGenerator;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    @Override
    @Transactional
    public Void handle(Command command) {

        AssetStorage.UploadResult result = assetStorage.handleUploadNotification(command.headers(), command.body());

        AssetId assetId = new AssetId(timeBasedIdGenerator.generate());
        Asset asset = new Asset(
            assetId,
            result.namespace(),
            result.fileHash(),
            new AssetDimension(
                result.dimension().getWidth(),
                result.dimension().getHeight()
            ),
            result.fileSize()
        );
        assetRepository.save(asset);


        ProfileId profileId = new ProfileId(Long.parseLong(result.context().get("profileId").toString()));
        Profile profile = profileRepository.findById(profileId)
            .orElseThrow(() -> new EntityNotFoundException("Profile not found with id: " + profileId));

        if(result.namespace().equals(Namespace.USER_AVATARS)) {
            profile.changeAvatar(asset.getFileHash());
        } else if(result.namespace().equals(Namespace.USER_BANNERS)) {
            profile.changeBanner(asset.getFileHash());
        }

        profileRepository.save(profile);

        String eventId = uniqueStringIdGenerator.generate();
		FileStorageUploadedIntegrationEvent event = new FileStorageUploadedIntegrationEvent(
            result.namespace().toString(),
            result.context(),
            result.fileHash(),
            result.dimension().getWidth(),
            result.dimension().getHeight(),
            result.fileSize()
        );

        IntegrationEventWrapper<FileStorageUploadedIntegrationEvent> wrapper = new IntegrationEventWrapper<>(eventId, null, event);
		integrationEventPublisher.publish(wrapper);
        return null;
    }
    
}
