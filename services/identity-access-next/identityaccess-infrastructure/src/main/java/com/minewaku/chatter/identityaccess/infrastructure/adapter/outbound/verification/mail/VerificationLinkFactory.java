package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.verification.mail;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/** Builds {@code {public-base-url}{confirm-path}?token={rawToken}} from configuration. */
@Component
public class VerificationLinkFactory {

    private final String base;
    private final String path;

    VerificationLinkFactory(VerificationMailProperties properties) {
        this.base = stripTrailingSlash(properties.publicBaseUrl());
        this.path =
                properties.confirmPath().startsWith("/") ? properties.confirmPath() : "/" + properties.confirmPath();
    }

    public String linkFor(String rawToken) {
        return base + path + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
