package com.minewaku.chatter.profile.application.service.command.profile;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.profile.application.exception.EntityNotFoundException;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.AssetUploadedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.UploadBannerUseCase;
import com.minewaku.chatter.profile.application.port.outbound.provider.AssetHasher;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.profile.domain.model.file.model.Asset;
import com.minewaku.chatter.profile.domain.model.file.model.AssetId;
import com.minewaku.chatter.profile.domain.model.file.model.ImageDimension;
import com.minewaku.chatter.profile.domain.model.file.model.Namespace;
import com.minewaku.chatter.profile.domain.model.file.repository.AssetRepository;
import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;
import com.minewaku.chatter.profile.domain.sharedkernel.service.TimeBasedIdGenerator;
import com.minewaku.chatter.profile.domain.sharedkernel.service.UniqueStringIdGenerator;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class UploadBannerApplicationService implements UploadBannerUseCase {

    private final AssetRepository assetRepository;
    private final ProfileRepository profileRepository;
    
    private final TimeBasedIdGenerator timeBasedIdGenerator;
    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    private final AssetHasher assetHasher;

    private final AssetStorage assetStorage;

    private final IntegrationEventPublisher integrationEventPublisher;


    public UploadBannerApplicationService(
                AssetRepository assetRepository,
                ProfileRepository profileRepository,

                TimeBasedIdGenerator timeBasedIdGenerator,
                UniqueStringIdGenerator uniqueStringIdGenerator,
                AssetHasher assetHasher,

                AssetStorage assetStorage,
            
                OutboxStore outboxStore) {


        this.assetRepository = assetRepository;
        this.profileRepository = profileRepository;
        
        this.timeBasedIdGenerator = timeBasedIdGenerator;
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
        this.assetHasher = assetHasher;

        this.assetStorage = assetStorage;
    
        this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
    }

    @Override
	@Retry(name = "transientDataAccess")
    @CacheEvict(value = "profiles", key = "#command.profileId().value()")
    @Transactional
    public Void handle(UploadBannerUseCase.Command command) {
        Profile profile = profileRepository.findById(command.profileId())
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        try {
            String hash = assetHasher.hash(command.banner().openStream());
            assetStorage.upload(command.banner(), hash);

            AssetId assetId = new AssetId(timeBasedIdGenerator.generate());
            Asset asset = new Asset(assetId, Namespace.USER_BANNERS, hash, new ImageDimension(command.banner().getWidth(), command.banner().getHeight()));
            assetRepository.save(asset);

            profile.changeBanner(hash);
            profileRepository.save(profile);

            
            String eventId = uniqueStringIdGenerator.generate();
            AssetUploadedIntegrationEvent assetUploadedIntegrationEvent = new AssetUploadedIntegrationEvent(
                assetId.getValue(),
                Namespace.USER_BANNERS.toString(),
                hash
            );
            IntegrationEventWrapper<AssetUploadedIntegrationEvent> wrapper = new IntegrationEventWrapper<>(eventId, assetId.getValue().toString(), assetUploadedIntegrationEvent);
            integrationEventPublisher.publish(wrapper);
        } catch(Exception e) {
            throw new RuntimeException("Failed to upload banner: " + e.getMessage(), e);
        }
        
        return null;
    }

    //store file directly to file storage but in a temp directory
    //if success then open transaction to:
    // - store file metadata to database
    // - store integration event to outbox
    //consume event eventually to move file from temp directory to final directory, if failed retry few times, if still failed then mark event as failed and require manual intervention to move file and mark event as completed
}
