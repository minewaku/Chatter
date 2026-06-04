package com.minewaku.chatter.profile.application.service.command.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.profile.application.exception.EntityNotFoundException;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.IntegrationEventPublisher;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.AssetDetachedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.profile.application.port.inbound.command.security.usecase.SoftDeleteProfileUseCase;
import com.minewaku.chatter.profile.domain.model.asset.model.AssetIdentity;
import com.minewaku.chatter.profile.domain.model.asset.model.Namespace;
import com.minewaku.chatter.profile.domain.model.asset.repository.AssetRepository;
import com.minewaku.chatter.profile.domain.model.profile.event.AvatarReplacedDomainEvent;
import com.minewaku.chatter.profile.domain.model.profile.event.BannerReplacedDomainEvent;
import com.minewaku.chatter.profile.domain.model.profile.model.Profile;
import com.minewaku.chatter.profile.domain.model.profile.repository.ProfileRepository;
import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.profile.domain.sharedkernel.service.UniqueStringIdGenerator;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class SoftDeleteProfileApplicationService implements SoftDeleteProfileUseCase {
	
	private final ProfileRepository profileRepository;
	private final AssetRepository assetRepository;
	private final UniqueStringIdGenerator uniqueStringIdGenerator;
	private final IntegrationEventPublisher integrationEventPublisher;

	public SoftDeleteProfileApplicationService(
				ProfileRepository profileRepository,
				AssetRepository assetRepository,
				UniqueStringIdGenerator uniqueStringIdGenerator,
				OutboxStore outboxStore) {

		this.profileRepository = profileRepository;
		this.assetRepository = assetRepository;
		this.uniqueStringIdGenerator = uniqueStringIdGenerator;
		this.integrationEventPublisher = new IntegrationEventPublisher(outboxStore);
	}

    @Override
	@Retry(name = "transientDataAccess")
	@CacheEvict(value = "profiles", key = "#command.profileId().value()")
	@Transactional
    public Void handle(Command command) {
		Profile profile = profileRepository.findById(command.profileId())
			.orElseThrow(() -> new EntityNotFoundException("Profile does not exist"));

		String hashAvatar = profile.getAvatarHash();
		String hashBanner = profile.getBannerHash();


		if (profile.softDelete(command.username(), command.enablement())) {
			profileRepository.save(profile);

			if(hashAvatar != null) {
				AssetIdentity assetIdentity = new AssetIdentity(Namespace.USER_AVATARS, hashAvatar);
				assetRepository.deleteByAssetIdentity(assetIdentity);
			}
			if(hashBanner != null) {
				AssetIdentity assetIdentity = new AssetIdentity(Namespace.USER_BANNERS, hashBanner);
				assetRepository.deleteByAssetIdentity(assetIdentity);
			}
		}

		List<IntegrationEventWrapper<AssetDetachedIntegrationEvent>> eventWrappers = new ArrayList<>();

		List<AvatarReplacedDomainEvent> avatarEvents = avatarReplacedDomainEventFiltered(profile.getDomainEvents());
		avatarEvents.forEach(event -> {

			String eventId = uniqueStringIdGenerator.generate();
			AssetDetachedIntegrationEvent integrationEvent = new AssetDetachedIntegrationEvent(
				event.getNamespace(),
				event.getOldHashFile()
			);

			IntegrationEventWrapper<AssetDetachedIntegrationEvent> eventWrapper = new IntegrationEventWrapper<>(
				eventId,
				event.getOldHashFile(),
				integrationEvent
			);
			eventWrappers.add(eventWrapper);
		});

		List<BannerReplacedDomainEvent> bannerEvents = bannerReplacedDomainEventFiltered(profile.getDomainEvents());
		bannerEvents.forEach(event -> {
			String eventId = uniqueStringIdGenerator.generate();
			AssetDetachedIntegrationEvent integrationEvent = new AssetDetachedIntegrationEvent(
				event.getNamespace(),
				event.getOldHashFile()
			);

			IntegrationEventWrapper<AssetDetachedIntegrationEvent> eventWrapper = new IntegrationEventWrapper<>(
				eventId,
				event.getOldHashFile(),
				integrationEvent
			);
			eventWrappers.add(eventWrapper);
		});

		integrationEventPublisher.publish(eventWrappers);
        return null;
	}

	private List<AvatarReplacedDomainEvent> avatarReplacedDomainEventFiltered(List<DomainEvent> events) {
        return events.stream()
                .filter(event -> event instanceof AvatarReplacedDomainEvent)
                .map(event -> (AvatarReplacedDomainEvent) event)
                .toList();
    }

	private List<BannerReplacedDomainEvent> bannerReplacedDomainEventFiltered(List<DomainEvent> events) {
        return events.stream()
                .filter(event -> event instanceof BannerReplacedDomainEvent)
                .map(event -> (BannerReplacedDomainEvent) event)
                .toList();
    }
}
