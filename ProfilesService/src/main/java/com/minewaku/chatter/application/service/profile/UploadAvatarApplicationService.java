package com.minewaku.chatter.application.service.profile;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.domain.command.profile.UploadFileCommand;
import com.minewaku.chatter.domain.model.StorageFile;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.profile.UploadAvatarUseCase;
import com.minewaku.chatter.domain.port.out.repository.ProfileRepository;
import com.minewaku.chatter.domain.port.out.service.FileStorage;
import com.minewaku.chatter.domain.port.out.service.FileStorageKeyGenerator;
import com.minewaku.chatter.domain.response.FileStorageResponse;
import com.minewaku.chatter.domain.value.file.InputAvatar;
import com.minewaku.chatter.domain.value.id.StorageKey;

public class UploadAvatarApplicationService implements UploadAvatarUseCase {

    private final FileStorage fileStorage;
    private final FileStorageKeyGenerator fileStorageKeyGenerator;
    private final ProfileRepository profileRepository;

    public UploadAvatarApplicationService(
            FileStorage fileStorage,
            FileStorageKeyGenerator fileStorageKeyGenerator,
            ProfileRepository profileRepository) {
        
        this.fileStorage = fileStorage;
        this.fileStorageKeyGenerator = fileStorageKeyGenerator;
        this.profileRepository = profileRepository;
    }


    @Override
    public Void handle(UploadFileCommand command) {

        User user = profileRepository.findActivatedByUserId(command.userId())
                .orElseThrow(() -> new EntityNotFoundException("User does not exist"));

        StorageKey key = new StorageKey(fileStorageKeyGenerator.generate());

        InputAvatar inputAvatar = new InputAvatar(
            key,
            user.getId(),
            command.inputImage().getOriginalFilename(),
            command.inputImage().getContentType(),
            command.inputImage().getSizeInBytes(),
            command.inputImage().getContentStream()
        );

        FileStorageResponse response = fileStorage.upload(inputAvatar);
        

        StorageFile avatar = new StorageFile(
            response.key(),
            response.uri()
        );
        user.setAvatar(avatar);

        profileRepository.uploadAvatarUrl(user);

        fileStorage.deleteByKey(user.getAvatar().getKey());
        return null;
    }
}
