package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.verification.mail;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("identity-access.verification.mail")
public record VerificationMailProperties(String from, String subject, String publicBaseUrl, String confirmPath) {

    public VerificationMailProperties {
        if (from == null || from.isBlank()) {
            from = "no-reply@chatter.local";
        }
        if (subject == null || subject.isBlank()) {
            subject = "Verify your Chatter email";
        }
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            publicBaseUrl = "http://localhost:5001";
        }
        if (confirmPath == null || confirmPath.isBlank()) {
            confirmPath = "/api/v1/auth/verification/confirm";
        }
        URI base = URI.create(publicBaseUrl);
        if (base.getScheme() == null || !base.getScheme().startsWith("http") || base.getHost() == null) {
            throw new IllegalArgumentException(
                    "identity-access.verification.mail.public-base-url must be an absolute http(s) URL");
        }
    }
}
