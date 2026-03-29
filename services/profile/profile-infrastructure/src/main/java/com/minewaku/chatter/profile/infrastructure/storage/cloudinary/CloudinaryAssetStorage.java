package com.minewaku.chatter.profile.infrastructure.storage.cloudinary;

import java.util.Map;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.profile.domain.model.file.model.Namespace;
import com.minewaku.chatter.profile.infrastructure.storage.cloudinary.property.CloudinaryProperties;

@Service
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryAssetStorage implements AssetStorage {

    private final Cloudinary cloudinary;

    public CloudinaryAssetStorage(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public void upload(StorableFile storableFile, String fileHash) {
        try {
            cloudinary.uploader().upload(
                storableFile.getInputStream(), 
                Map.of(
                    "folder", namespaceToFolder(storableFile.getNamespace()), 
                    "public_id", fileHash,
                    "overwrite", true));

        } catch(Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String namespaceToFolder(Namespace namespace) {
        return switch(namespace) {
            case USER_AVATARS -> "chatter/profile/avatars";
            case USER_BANNERS -> "chatter/profile/banners";
        };
    }
}
