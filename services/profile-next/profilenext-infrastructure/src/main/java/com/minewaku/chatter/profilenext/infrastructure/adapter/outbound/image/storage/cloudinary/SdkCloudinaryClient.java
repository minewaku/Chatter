package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.exceptions.RateLimited;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The SDK-backed client. Provider errors are translated; the SDK's own messages (which may echo requests) are not. */
final class SdkCloudinaryClient implements CloudinaryClient {

    private static final int MAX_PAGE_SIZE = 500;

    private final Cloudinary cloudinary;

    SdkCloudinaryClient(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String sign(Map<String, Object> params) {
        return cloudinary.apiSignRequest(
                params, cloudinary.config.apiSecret, cloudinary.config.signatureVersion);
    }

    @Override
    public void destroy(String publicId) {
        Map<?, ?> result;
        try {
            result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image", "invalidate", true));
        } catch (IOException | RuntimeException e) {
            throw new ProviderFailureException("image removal did not reach the provider");
        }
        Object outcome = result == null ? null : result.get("result");
        if (!"ok".equals(outcome) && !"not found".equals(outcome)) {
            throw new ProviderFailureException("the provider did not confirm the removal");
        }
    }

    @Override
    public ResourcePage listResources(String prefix, String cursor) {
        Map<String, Object> options = new java.util.HashMap<>();
        options.put("type", "upload");
        options.put("prefix", prefix);
        options.put("max_results", MAX_PAGE_SIZE);
        if (cursor != null) {
            options.put("next_cursor", cursor);
        }
        Map<?, ?> response;
        try {
            response = cloudinary.api().resources(options);
        } catch (RateLimited e) {
            throw new ProviderRateLimitedException("the provider asked to back off");
        } catch (Exception e) {
            throw new ProviderFailureException("the image listing did not reach the provider");
        }
        List<Resource> resources = new ArrayList<>();
        Object items = response.get("resources");
        if (items instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map && map.get("public_id") != null && map.get("created_at") != null) {
                    resources.add(new Resource(
                            map.get("public_id").toString(),
                            Instant.parse(map.get("created_at").toString())));
                }
            }
        }
        Object next = response.get("next_cursor");
        return new ResourcePage(resources, next == null ? null : next.toString());
    }
}
