package com.minewaku.chatter.profile.application.service.command.profile;

import com.minewaku.chatter.profile.application.exception.EntityNotFoundException;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.UploadAvatarUseCase;
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



public class UploadAvatarApplicationService implements UploadAvatarUseCase {
    
    private final AssetRepository assetRepository;
    private final ProfileRepository profileRepository;
    
    private final TimeBasedIdGenerator timeBasedIdGenerator;
    private final AssetHasher assetHasher;

    private final AssetStorage assetStorage;

    public UploadAvatarApplicationService(
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
    public Void handle(UploadAvatarUseCase.Command command) {
        Profile profile = profileRepository.findById(command.profileId())
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        String hash = assetHasher.hash(command.avatar().getInputStream());
        assetStorage.upload(command.avatar(), hash);

        AssetId assetId = new AssetId(timeBasedIdGenerator.generate());
        Asset asset = new Asset(assetId, Namespace.USER_AVATARS, hash, new ImageDimension(command.avatar().getWidth(), command.avatar().getHeight()));
        assetRepository.save(asset);

        profile.changeAvatar(hash);
        profileRepository.save(profile);

        return null;
    }
}
