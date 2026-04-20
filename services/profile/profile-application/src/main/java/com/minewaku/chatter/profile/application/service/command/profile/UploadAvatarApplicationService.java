package com.minewaku.chatter.profile.application.service.command.profile;

import org.springframework.stereotype.Service;

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


@Service
public class UploadAvatarApplicationService implements UploadAvatarUseCase{

    private final ProfileRepository profileRepository;
    private final AssetRepository assetRepository;
    private final AssetStorage assetStorage;
    private final TimeBasedIdGenerator timeBasedIdGenerator;
    private final AssetHasher  assetHasher;

    public UploadAvatarApplicationService(
        ProfileRepository profileRepository,
        AssetRepository assetRepository,
        AssetStorage assetStorage,
        TimeBasedIdGenerator timeBasedIdGenerator,
        AssetHasher assetHasher
    ) {
        this.profileRepository = profileRepository;
        this.assetRepository = assetRepository;
        this.assetStorage = assetStorage;
        this.timeBasedIdGenerator = timeBasedIdGenerator;
        this.assetHasher = assetHasher;
    }

    @Override
    public Void handle(UploadAvatarUseCase.Command command) {
        
        // 1. Validate sớm (Fast-fail)
        Profile profile = profileRepository.findById(command.profileId())
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        try {
            String hash = assetHasher.hash(command.avatar().openStream());
            assetStorage.upload(command.avatar(), hash);

            AssetId assetId = new AssetId(timeBasedIdGenerator.generate());
            Asset asset = new Asset(assetId, Namespace.USER_AVATARS, hash, new ImageDimension(command.avatar().getWidth(), command.avatar().getHeight()));
            assetRepository.save(asset);

            profile.changeAvatar(hash);
            profileRepository.save(profile);
        } catch(Exception e) {
            throw new RuntimeException("Failed to upload avatar: " + e.getMessage(), e);
        }

        return null;
    }
}
