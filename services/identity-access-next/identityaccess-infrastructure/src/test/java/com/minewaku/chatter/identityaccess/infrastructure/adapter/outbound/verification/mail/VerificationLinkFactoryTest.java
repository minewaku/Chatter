package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.verification.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VerificationLinkFactoryTest {

    @Test
    void buildsBaseUrlPathAndTokenWithDefaults() {
        var factory = new VerificationLinkFactory(new VerificationMailProperties(null, null, null, null));

        assertThat(factory.linkFor("tok123"))
                .isEqualTo("http://localhost:5001/api/v1/auth/verification/confirm?token=tok123");
    }

    @Test
    void honoursTheConfiguredPublicBaseUrl() {
        var factory = new VerificationLinkFactory(
                new VerificationMailProperties(null, null, "https://chatter.example.com", "/verify"));

        assertThat(factory.linkFor("abc")).isEqualTo("https://chatter.example.com/verify?token=abc");
    }

    @Test
    void normalisesTrailingSlashAndMissingLeadingSlash() {
        var factory = new VerificationLinkFactory(
                new VerificationMailProperties(null, null, "https://chatter.example.com/", "verify"));

        assertThat(factory.linkFor("abc")).isEqualTo("https://chatter.example.com/verify?token=abc");
    }

    @Test
    void encodesTheToken() {
        var factory = new VerificationLinkFactory(new VerificationMailProperties(null, null, null, null));

        assertThat(factory.linkFor("a+b/c d")).endsWith("?token=a%2Bb%2Fc+d");
    }

    @Test
    void rejectsABaseUrlThatIsNotAnAbsoluteHttpUrl() {
        assertThatThrownBy(() -> new VerificationMailProperties(null, null, "not-a-url", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identity-access.verification.mail.public-base-url");
    }
}
