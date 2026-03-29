package com.minewaku.chatter.profile.application.service.command.profile;

import com.minewaku.chatter.profile.application.exception.EntityNotFoundException;
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


public class UploadBannerApplicationService implements UploadBannerUseCase {

    private final AssetRepository assetRepository;
    private final ProfileRepository profileRepository;
    
    private final TimeBasedIdGenerator timeBasedIdGenerator;
    private final AssetHasher assetHasher;

    private final AssetStorage assetStorage;


    public UploadBannerApplicationService(
                AssetRepository assetRepository,
                ProfileRepository profileRepository,

                TimeBasedIdGenerator timeBasedIdGenerator,
                AssetHasher assetHasher,

                AssetStorage assetStorage) {


        this.assetRepository = assetRepository;
        this.profileRepository = profileRepository;
        
        this.timeBasedIdGenerator = timeBasedIdGenerator;
        this.assetHasher = assetHasher;

        this.assetStorage = assetStorage;
    }

    @Override
    public Void handle(UploadBannerUseCase.Command command) {
        Profile profile = profileRepository.findById(command.profileId())
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        String hash = assetHasher.hash(command.banner().getInputStream());
        assetStorage.upload(command.banner(), hash);

        AssetId assetId = new AssetId(timeBasedIdGenerator.generate());
        Asset asset = new Asset(assetId, Namespace.USER_BANNERS, hash, new ImageDimension(command.banner().getWidth(), command.banner().getHeight()));
        assetRepository.save(asset);

        profile.changeBanner(hash);
        profileRepository.save(profile);

        return null;
    }
}
