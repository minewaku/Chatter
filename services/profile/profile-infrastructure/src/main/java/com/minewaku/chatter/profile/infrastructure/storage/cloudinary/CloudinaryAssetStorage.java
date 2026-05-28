package com.minewaku.chatter.profile.infrastructure.storage.cloudinary;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.profile.domain.model.file.model.Namespace;
import com.minewaku.chatter.profile.infrastructure.storage.cloudinary.property.CloudinaryProperties;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryAssetStorage implements AssetStorage {

    private final Cloudinary cloudinary;

    public CloudinaryAssetStorage(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public void delete(Namespace namespace, String fileHash) {
        try {
            cloudinary.uploader().destroy(
                buildPublicId(namespaceToTempFolder(namespace), fileHash),
                Map.of()
            );
        } catch(Exception e) {  
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public UploadSignature generateUploadSignature(Namespace namespace, Map<String, Object> params) {
        
        long timestamp = System.currentTimeMillis() / 1000L;
        String folder = namespaceToTempFolder(namespace);
        String uploadPreset = namespaceToPreset(namespace);

        Map<String, Object> uploadParams = buildContext(namespace, params);
        uploadParams.put("namespace", namespace.name());
        String context = cloudinaryContextGenerator(uploadParams);

        Map<String, Object> paramsToSign = new HashMap<>();
        paramsToSign.put("timestamp", timestamp);
        paramsToSign.put("folder", folder);
        paramsToSign.put("upload_preset", uploadPreset);
        paramsToSign.put("tags", namespace.name()); 
        paramsToSign.put("context", context);


        try {
            String apiSecret = cloudinary.config.apiSecret;
            String apiKey = cloudinary.config.apiKey;
            String cloudName = cloudinary.config.cloudName;

            String signature = cloudinary.apiSignRequest(paramsToSign, apiSecret);
            Map<String, Object> payload = new HashMap<>();
            payload.put("folder", folder);
            payload.put("upload_preset", uploadPreset);
            payload.put("timestamp", timestamp);
            payload.put("signature", signature);
            payload.put("api_key", apiKey);
            payload.put("tags", namespace.name());
            payload.put("context", context);

            String targetUrl = String.format("https://api.cloudinary.com/v1_1/%s/auto/upload", cloudName);

            return new UploadSignature(
                targetUrl,
                "POST",
                payload
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Cloudinary upload signature", e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public UploadResult handleUploadNotification(Map<String, String> headers, Map<String, Object> body) {
        String publicId = (String) body.get("public_id");
        String fileHash = extractHashFromPublicId(publicId);
        Map<String, Object> contextPayload = (Map<String, Object>) body.get("context");
        Map<String, Object> customContext = (Map<String, Object>) contextPayload.get("custom");
        
        String namespace = Objects.requireNonNull(
            (String) customContext.get("namespace"), "missing namespace in context"
        );
        
        int width = (int) body.get("width");
        int height = (int) body.get("height");
        int fileSize = (int) body.get("bytes");

        Map<String, Object> contextMap = buildContext(Namespace.valueOf(namespace), customContext);

        return new UploadResult(
            fileHash,
            namespace,
            contextMap,
            width,
            height,
            fileSize
        );
    }

    @Override
    public void commitUpload(Namespace namespace, String fileHash) {
        String permanentPublicId = getPermanentFolder(namespace, fileHash);
        String tempPublicId = getTempFolder(namespace, fileHash);

        try {
            cloudinary.uploader().rename(tempPublicId, permanentPublicId, Map.of("overwrite", true));
        } catch(Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    //PRIVATE HELPER METHODS
    //BUILD FOLDER PATHS BASED ON NAMESPACE AND CONTEXT
    private String getTempFolder(
                Namespace namespace,
                String fileHash) {

        return switch(namespace) {
            case USER_AVATARS -> { 
                yield "chatter/temp/profile/avatars" + "/" + fileHash;
            }
            case USER_BANNERS -> {
                yield "chatter/temp/profile/banners" + "/" + fileHash;
            }
        };
    }

    private String getPermanentFolder(
                Namespace namespace,
                String fileHash) {

        return switch(namespace) {
            case USER_AVATARS -> { 
                yield "chatter/permanent/profile/avatars" + "/" + fileHash;
            }
            case USER_BANNERS -> {
                yield "chatter/permanent/profile/banners" + "/" + fileHash;
            }
        };
    }

    private String namespaceToTempFolder(
                Namespace namespace) {

        return switch(namespace) {
            case USER_AVATARS -> { 
                yield "chatter/temp/profile/avatars";
            }
            case USER_BANNERS -> {
                yield "chatter/temp/profile/banners";
            }
        };
    }


    //OTHER HELPER METHODS
    private String buildPublicId(String folder, String fileHash) {
        return folder + "/" + fileHash;
    }

    private String extractHashFromPublicId(String publicId) {
        String[] parts = publicId.split("/");
        return parts[parts.length - 1];
    }

    private String namespaceToPreset(Namespace namespace) {
        return switch(namespace) {
            case USER_AVATARS -> "upload_avatar";
            case USER_BANNERS -> "upload_banner";
        };
    }

    private String cloudinaryContextGenerator(Map<String, Object> context) {
        return context.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("|"));
    }


    //BUILD CONTEXT HELPER METHODS
    private Map<String, Object> buildContext(Namespace namespace, Map<String, Object> requestContext) {
        return switch(namespace) {
            case USER_AVATARS -> buildProfileImageContext(requestContext);
            case USER_BANNERS -> buildProfileImageContext(requestContext);
        };
    }

    private Map<String, Object> buildProfileImageContext(Map<String, Object> requestContext) {
        String profileId = (String) requestContext.get("profileId");

        return new HashMap<>(Map.of(
            "profileId", profileId
        ));
    }
}