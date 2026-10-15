package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SecureRandomRefreshTokenGeneratorTest {

    private final SecureRandomRefreshTokenGenerator generator = new SecureRandomRefreshTokenGenerator();

    @Test
    void tokenIsUnpaddedBase64UrlOf32Bytes() {
        String token = generator.generate();

        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);
    }

    @Test
    void tenThousandTokensAreUnique() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            tokens.add(generator.generate());
        }

        assertThat(tokens).hasSize(10_000);
    }
}
