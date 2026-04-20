package com.minewaku.chatter.profile.infrastructure.storage.cloudinary;

import java.util.HashMap;
import java.util.List;
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
                storableFile.openStream(),
                Map.of(
                    "folder", namespaceToFolder(storableFile.getNamespace()), 
                    "public_id", fileHash,
                    "overwrite", true));

        } catch(Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void delete(String fileHash) {
        try {
            cloudinary.uploader().destroy(
                fileHash,
                Map.of());
        } catch(Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String namespaceToFolder(Namespace namespace) {
        return switch(namespace) {
            case USER_AVATARS -> "chatter/temp/profile/avatars";
            case USER_BANNERS -> "chatter/temp/profile/banners";
        };
    }

    private String namespaceToPreset(Namespace namespace) {
        return switch(namespace) {
            case USER_AVATARS -> "upload_avatar";
            case USER_BANNERS -> "upload_banner";
        };
    }

    @Override
    public AssetStorage.Response generateUploadSignature(Namespace namespace) {
        
        long timestamp = System.currentTimeMillis() / 1000L;
        String folder = namespaceToFolder(namespace);
        
        List<String> allowedFormatsList = List.of("jpg", "png", "jpeg", "webp");
        String allowedFormatsStr = String.join(",", allowedFormatsList);
        String uploadPreset = namespaceToPreset(namespace);

        Map<String, Object> paramsToSign = new HashMap<>();
        paramsToSign.put("timestamp", timestamp);
        paramsToSign.put("folder", folder);
        paramsToSign.put("allowed_formats", allowedFormatsStr);
        paramsToSign.put("upload_preset", uploadPreset);

        try {
            String apiSecret = cloudinary.config.apiSecret;
            String apiKey = cloudinary.config.apiKey;
            String cloudName = cloudinary.config.cloudName;

            String signature = cloudinary.apiSignRequest(paramsToSign, apiSecret);
            Map<String, Object> payload = new HashMap<>();
            payload.put("folder", folder);
            payload.put("allowed_formats", allowedFormatsStr);
            payload.put("upload_preset", uploadPreset);
            payload.put("timestamp", timestamp);
            payload.put("signature", signature);
            payload.put("api_key", apiKey);

            String targetUrl = String.format("https://api.cloudinary.com/v1_1/%s/auto/upload", cloudName);

            return new AssetStorage.Response(
                targetUrl,
                "POST",
                payload
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Cloudinary upload signature", e);
        }
    }
}
