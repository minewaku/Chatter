package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.cloudinary;

import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageRateLimitedException;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profilenext.profile.internal.model.ConfirmedUpload;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.StoredImageInfo;
import com.minewaku.chatter.profilenext.profile.internal.model.StoredImagePage;
import com.minewaku.chatter.profilenext.profile.internal.model.UploadPermission;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The only adapter for {@link ImageStorage}. Uploads go straight from the client to Cloudinary with a signed,
 * server-chosen public id; Cloudinary then calls the webhook, whose signature is checked here over the raw bytes.
 */
@Component
public class CloudinaryImageStorage implements ImageStorage {

    static final String PREFIX = "chatter/profile/";
    private static final Pattern PUBLIC_ID = Pattern.compile("^chatter/profile/(avatars|banners)/([0-9]+)/([^/]+)$");

    private final CloudinaryClient client;
    private final ImageProperties properties;
    private final JsonMapper json = JsonMapper.builder().build();

    CloudinaryImageStorage(CloudinaryClient client, ImageProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public UploadPermission permit(UserId userId, ImageSlot slot, String uploadId, Instant now) {
        if (!properties.credentialsConfigured()) {
            throw new ImageStorageUnavailableException("image storage is not configured");
        }
        Map<String, Object> signed = new LinkedHashMap<>();
        signed.put("public_id", PREFIX + slot.folder() + "/" + userId.value() + "/" + uploadId);
        signed.put("allowed_formats", String.join(",", properties.allowedFormats()));
        signed.put("context", "userId=" + userId.value() + "|slot=" + slot.name());
        signed.put("timestamp", Long.toString(now.getEpochSecond()));
        if (!properties.webhookUrl().isBlank()) {
            signed.put("notification_url", properties.webhookUrl());
        }
        String signature;
        try {
            signature = client.sign(signed);
        } catch (RuntimeException e) {
            throw new ImageStorageUnavailableException("the upload could not be signed");
        }
        Map<String, String> fields = new LinkedHashMap<>();
        signed.forEach((name, value) -> fields.put(name, value.toString()));
        fields.put("api_key", properties.apiKey());
        fields.put("signature", signature);
        return new UploadPermission(
                "https://api.cloudinary.com/v1_1/" + properties.cloudName() + "/image/upload",
                "POST",
                fields,
                now.plus(properties.permissionLifetime()));
    }

    @Override
    public Optional<ConfirmedUpload> verify(byte[] rawBody, String signature, String timestamp, Instant now) {
        if (rawBody == null || signature == null || timestamp == null || !properties.credentialsConfigured()) {
            return Optional.empty();
        }
        if (!signatureMatches(rawBody, signature, timestamp) || !fresh(timestamp, now)) {
            return Optional.empty();
        }
        try {
            return parse(json.readTree(rawBody));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private boolean signatureMatches(byte[] rawBody, String signature, String timestamp) {
        try {
            MessageDigest digest = MessageDigest.getInstance(properties.signatureAlgorithm());
            ByteArrayOutputStream signed = new ByteArrayOutputStream();
            signed.writeBytes(rawBody);
            signed.writeBytes(timestamp.getBytes(StandardCharsets.UTF_8));
            signed.writeBytes(properties.apiSecret().getBytes(StandardCharsets.UTF_8));
            byte[] expected = HexFormat.of().formatHex(digest.digest(signed.toByteArray())).getBytes(StandardCharsets.UTF_8);
            byte[] given = signature.trim().toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expected, given);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("digest algorithm unavailable", e);
        }
    }

    private boolean fresh(String timestamp, Instant now) {
        try {
            Instant sent = Instant.ofEpochSecond(Long.parseLong(timestamp.trim()));
            Duration skew = Duration.between(sent, now).abs();
            return skew.compareTo(properties.webhookTolerance()) <= 0;
        } catch (NumberFormatException | java.time.DateTimeException e) {
            return false;
        }
    }

    private Optional<ConfirmedUpload> parse(JsonNode body) {
        if (!"upload".equals(text(body, "notification_type"))) {
            return Optional.empty();
        }
        String publicId = text(body, "public_id");
        String url = text(body, "secure_url");
        String format = text(body, "format");
        if (publicId == null || url == null || format == null || !body.path("bytes").canConvertToLong()) {
            return Optional.empty();
        }
        Matcher layout = PUBLIC_ID.matcher(publicId);
        if (!layout.matches()) {
            return Optional.empty();
        }
        ImageSlot slot = layout.group(1).equals(ImageSlot.AVATAR.folder()) ? ImageSlot.AVATAR : ImageSlot.BANNER;
        long userId;
        try {
            userId = Long.parseLong(layout.group(2));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        JsonNode custom = body.path("context").path("custom");
        if (!Long.toString(userId).equals(custom.path("userId").asString(null))
                || !slot.name().equals(custom.path("slot").asString(null))) {
            return Optional.empty();
        }
        return Optional.of(new ConfirmedUpload(
                UserId.of(userId), slot, publicId, url, format, body.path("bytes").asLong()));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isString() && !value.asString().isBlank() ? value.asString() : null;
    }

    @Override
    public void remove(String publicId) {
        try {
            client.destroy(publicId);
        } catch (ProviderFailureException e) {
            throw new ImageStorageUnavailableException("image removal failed", e);
        }
    }

    @Override
    public StoredImagePage list(String cursor) {
        try {
            CloudinaryClient.ResourcePage page = client.listResources(PREFIX, cursor);
            List<StoredImageInfo> items = page.resources().stream()
                    .map(resource -> new StoredImageInfo(resource.publicId(), resource.createdAt()))
                    .toList();
            return new StoredImagePage(items, page.nextCursor());
        } catch (ProviderRateLimitedException e) {
            throw new ImageStorageRateLimitedException("the provider asked to back off", e);
        } catch (ProviderFailureException e) {
            throw new ImageStorageUnavailableException("the image listing failed", e);
        }
    }
}
