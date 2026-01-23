package com.minewaku.chatter.adapter.config.service;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;
import com.minewaku.chatter.adapter.config.properties.VaultCloudinaryProperties;

@Configuration
public class CloudinaryConfig {

    private final VaultCloudinaryProperties vaultCloudinaryProperties;

    public CloudinaryConfig(VaultCloudinaryProperties vaultCloudinaryProperties) {
        this.vaultCloudinaryProperties = vaultCloudinaryProperties;
    }


    @Bean
    public Cloudinary cloudinary() {
        try {
            return new Cloudinary(Map.of(
                "cloud_name", vaultCloudinaryProperties.getCloud_name(),
                "api_key", vaultCloudinaryProperties.getApi_key(),
                "api_secret", vaultCloudinaryProperties.getApi_secret(),
                "secure", true));
        } catch(Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
