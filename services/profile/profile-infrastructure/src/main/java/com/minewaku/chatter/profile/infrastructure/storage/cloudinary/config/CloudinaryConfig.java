package com.minewaku.chatter.profile.infrastructure.storage.cloudinary.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.minewaku.chatter.profile.infrastructure.storage.cloudinary.property.CloudinaryProperties;

@Configuration
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryConfig {

    private final CloudinaryProperties cloudinaryProperties;

    public CloudinaryConfig(CloudinaryProperties cloudinaryProperties) {
        this.cloudinaryProperties = cloudinaryProperties;
    }

    @Bean
    Cloudinary cloudinary() {
        try {
            return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudinaryProperties.getCloudName(),
                "api_key", cloudinaryProperties.getApiKey(),
                "api_secret", cloudinaryProperties.getApiSecret(),
                "secure", cloudinaryProperties.isSecure()));
        } catch(Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
