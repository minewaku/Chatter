package com.minewaku.chatter.profilenext.presentation.image;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Builds the address an image is shown at: {@code {publicBaseUrl}/{path}}, where the path is the provider-neutral one
 * the core reports. An image id is part of the path and never changes the bytes behind it, so every address can be
 * cached for good and a replaced image is simply a new address.
 */
@Component
public class ImageUrlResolver {

    private final String baseUrl;

    public ImageUrlResolver(@Value("${profile-next.images.public-base-url:}") String publicBaseUrl) {
        String trimmed = publicBaseUrl == null ? "" : publicBaseUrl.strip();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        this.baseUrl = trimmed;
    }

    /** The address for a relative path, or null when the slot is empty (no path). */
    public String resolve(String path) {
        return path == null ? null : baseUrl + "/" + path;
    }
}
