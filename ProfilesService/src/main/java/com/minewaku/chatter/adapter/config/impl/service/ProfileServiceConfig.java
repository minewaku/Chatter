package com.minewaku.chatter.adapter.config.impl.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.minewaku.chatter.application.service.profile.FindActivatedProfileByUsernameApplicationService;
import com.minewaku.chatter.application.service.profile.UpdateProfileApplicationService;
import com.minewaku.chatter.application.service.profile.UploadAvatarApplicationService;
import com.minewaku.chatter.application.service.profile.UploadBannerApplicationService;
import com.minewaku.chatter.domain.port.out.repository.ProfileRepository;
import com.minewaku.chatter.domain.port.out.service.FileStorage;
import com.minewaku.chatter.domain.port.out.service.FileStorageKeyGenerator;

@Configuration
public class ProfileServiceConfig {
    
    @Bean
    UpdateProfileApplicationService updateProfileApplicationService(ProfileRepository profileRepository) {
        return new UpdateProfileApplicationService(profileRepository);
    }


    @Bean
    FindActivatedProfileByUsernameApplicationService findActivatedProfileByUsernameApplicationService(
            ProfileRepository profileRepository) {
        return new FindActivatedProfileByUsernameApplicationService(profileRepository);
    }

    @Bean
    UploadAvatarApplicationService uploadProfileAvatarService(
            FileStorage fileStorage,
            FileStorageKeyGenerator fileStorageKeyGenerator,
            ProfileRepository profileRepository) {

        return new UploadAvatarApplicationService(fileStorage, fileStorageKeyGenerator, profileRepository);
    }

    @Bean
    UploadBannerApplicationService uploadProfileBannerService(
            FileStorage fileStorage,
            FileStorageKeyGenerator fileStorageKeyGenerator,
            ProfileRepository profileRepository) {
        return new UploadBannerApplicationService(fileStorage, fileStorageKeyGenerator, profileRepository);
    }

}
