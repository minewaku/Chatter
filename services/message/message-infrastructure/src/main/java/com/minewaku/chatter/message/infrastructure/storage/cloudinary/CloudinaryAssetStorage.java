package com.minewaku.chatter.message.infrastructure.storage.cloudinary;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.minewaku.chatter.message.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.message.domain.model.asset.model.Namespace;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.message.model.MessageId;
import com.minewaku.chatter.message.infrastructure.storage.cloudinary.property.CloudinaryProperties;

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
    public void delete(Namespace namespace, String fileHash, Map<String, Object> params) {
        try {
            cloudinary.uploader().destroy(
                buildPublicId(namespaceToTempFolder(namespace, params), fileHash),
                Map.of()
            );
        } catch(Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public UploadSignature generateUploadSignature(Namespace namespace, Map<String, Object> params) {
        
        long timestamp = System.currentTimeMillis() / 1000L;
        String folder = namespaceToTempFolder(namespace, params);
        
        String uploadPreset = namespaceToPreset(namespace);
        String context = cloudinaryContextGenerator(
            "channelId=" + params.get("channelId"),
            "messageId=" + params.get("messageId"), 
            "namespace=" + namespace.name());

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
        Map<String, Object> contextPayload = (Map<String, Object>) body.get("context");
        Map<String, Object> customContext = (Map<String, Object>) contextPayload.get("custom");
        
        String namespace = Objects.requireNonNull(
            (String) customContext.get("namespace"), "missing namespace in context"
        );
        
        String messageIdString = Objects.requireNonNull(
            (String) customContext.get("messageId"), "missing messageId in context"
        );

        String channelIdString = Objects.requireNonNull(
            (String) customContext.get("channelId"), "missing channelId in context"
        );

        int width = (int) body.get("width");
        int height = (int) body.get("height");
        int fileSize = (int) body.get("bytes");

        Map<String, Object> contextMap = Map.of("messageId", messageIdString, "channelId", channelIdString);

        return new UploadResult(
            publicId,
            namespace,
            contextMap,
            width,
            height,
            fileSize
        );
    }

    @Override
    public void commitUpload(Namespace namespace, String fileHash, Map<String, Object> params) {
        String permanentPublicId = getPermanentFolder(namespace, fileHash, params);
        String tempPublicId = getTempFolder(namespace, fileHash, params);

        try {
            cloudinary.uploader().rename(tempPublicId, permanentPublicId, Map.of("overwrite", true));
        } catch(Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String getTempFolder(
                Namespace namespace,
                String fileHash,
                Map<String, Object> params) {

        return switch(namespace) {
            case GUILD_ICON -> { 
                GuildId guildId = new GuildId(Long.parseLong(Objects.requireNonNull(
                    (String) params.get("guildId"), "missing guildId in " + params.keySet())));

                yield "chatter/temp/guilds/" + String.valueOf(guildId.getValue()) + "/icon" + "/" + fileHash;
            }

            case ATTACHMENT -> {
                ChannelId channelId = new ChannelId(Long.parseLong(Objects.requireNonNull(
                    (String) params.get("channelId"), "missing channelId in " + params.keySet())));
                MessageId messageId = new MessageId(Long.parseLong(Objects.requireNonNull(
                    (String) params.get("messageId"), "missing messageId in " + params.keySet())));

                yield "chatter/temp/channels/" + String.valueOf(channelId.getValue()) + "/attachments" + "/" + String.valueOf(messageId.getValue()) + "/" + fileHash;
            }
        };
    }

    private String getPermanentFolder(
                Namespace namespace,
                String fileHash,
                Map<String, Object> params) {

        return switch(namespace) {
            case GUILD_ICON -> { 
                GuildId guildId = new GuildId(Long.parseLong(Objects.requireNonNull(
                    (String) params.get("guildId"), "missing guildId in " + params.keySet())));

                yield "chatter/permanent/guilds/" + String.valueOf(guildId.getValue()) + "/icon" + "/" + fileHash;
            }

            case ATTACHMENT -> {
                ChannelId channelId = new ChannelId(Long.parseLong(Objects.requireNonNull(
                    (String) params.get("channelId"), "missing channelId in " + params.keySet())));
                MessageId messageId = new MessageId(Long.parseLong(Objects.requireNonNull(
                    (String) params.get("messageId"), "missing messageId in " + params.keySet())));

                yield "chatter/permanent/channels/" + String.valueOf(channelId.getValue()) + "/attachments" + "/" + String.valueOf(messageId.getValue()) + "/" + fileHash;
            }
        };
    }

    private String namespaceToTempFolder(
                Namespace namespace,
                Map<String, Object> params) {

        return switch(namespace) {
            case GUILD_ICON -> { 
                GuildId guildId = new GuildId(Long.parseLong(Objects.requireNonNull(
                    (String) params.get("guildId"), "missing guildId in " + params.keySet())));

                yield "chatter/temp/guilds/" + String.valueOf(guildId.getValue()) + "/icon";
            }

            case ATTACHMENT -> {
                ChannelId channelId = new ChannelId(Long.parseLong(Objects.requireNonNull(
                    (String) params.get("channelId"), "missing channelId in " + params.keySet())));
                MessageId messageId = new MessageId(Long.parseLong(Objects.requireNonNull(
                    (String) params.get("messageId"), "missing messageId in " + params.keySet())));

                yield "chatter/temp/channels/" + String.valueOf(channelId.getValue()) + "/attachments" + "/" + String.valueOf(messageId.getValue());
            }
        };
    }

    private String buildPublicId(String folder, String fileHash) {
        return folder + "/" + fileHash;
    }

    private String namespaceToPreset(Namespace namespace) {
        return switch(namespace) {
            case ATTACHMENT -> "upload_attachment";
            case GUILD_ICON -> "upload_guild_icon";
        };
    }

    private String cloudinaryContextGenerator(String... context) {
        return String.join("|", context);
    }

    private Map<String, Object> contextParser(String context) {
        Map<String, Object> parsedContext = new HashMap<>();
        String[] pairs = context.split("\\|");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                parsedContext.put(keyValue[0], keyValue[1]);
            }
        }

        return parsedContext;
    }
}

// {
//   "notification_type": "upload",
//   "timestamp": "2024-06-25T10:21:27+00:00",
//   "request_id": "c71a396e9526e0e37b92641a12345678",
//   "asset_id": "ab12cd34ef56gh78ij90kl12mn34op56",
//   "public_id": "abcxyz_hash_123",
//   "version": 1719310887,
//   "version_id": "a1b2c3d4e5f6g7h8i9j0",
//   "signature": "f2292bdc1b0c99e16cb677af81dc94da6e36eae3",
//   "width": 1920,
//   "height": 1080,
//   "format": "png",
//   "resource_type": "image",
//   "created_at": "2024-06-25T10:21:27Z",
//   "tags": [],
//   "bytes": 245012,
//   "type": "upload",
//   "etag": "d41d8cd98f00b204e9800998ecf8427e",
//   "placeholder": false,
//   "url": "http://res.cloudinary.com/your_cloud_name/image/upload/v1719310887/chatter/temp/message/avatars/123/abcxyz_hash_123.png",
//   "secure_url": "https://res.cloudinary.com/your_cloud_name/image/upload/v1719310887/chatter/temp/message/avatars/123/abcxyz_hash_123.png",
//   "folder": "chatter/temp/message/avatars/123",
//   "original_filename": "anh_bia_moi",
//   "api_key": "123456789012345"
// }

// POST /api/webhooks/storage HTTP/1.1
// Content-Type: application/json
// X-Cld-Timestamp: 1719310887
// X-Cld-Signature: f2292bdc1b0c99e16cb677af81dc94da6e36eae3