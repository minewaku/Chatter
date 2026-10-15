package com.minewaku.chatter.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.infrastructure.support.RsaKeyFixtures;
import java.security.KeyPair;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class KeyFailureStartupTest {

    @Configuration
    @EnableConfigurationProperties(JwtProperties.class)
    @Import(JwtDecoderConfiguration.class)
    static class Config {}

    private static final KeyPair PAIR = RsaKeyFixtures.newKeyPair();
    private static final String PRIVATE = RsaKeyFixtures.privatePem(PAIR);
    private static final String PUBLIC = RsaKeyFixtures.publicPem(PAIR);

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    private static String rootMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        for (Throwable t = failure; t != null; t = t.getCause()) {
            messages.append(t.getMessage()).append('\n');
        }
        return messages.toString();
    }

    @Test
    void validKeysStartTheContext() {
        runner.withPropertyValues(
                        "identity-access.security.jwt.private-key=" + PRIVATE,
                        "identity-access.security.jwt.public-key=" + PUBLIC)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void missingPrivateKeyStopsStartupAndNamesTheProperty() {
        runner.withPropertyValues("identity-access.security.jwt.public-key=" + PUBLIC)
                .run(context -> assertThat(rootMessages(context.getStartupFailure()))
                        .contains("identity-access.security.jwt.private-key"));
    }

    @Test
    void missingPublicKeyStopsStartupAndNamesTheProperty() {
        runner.withPropertyValues("identity-access.security.jwt.private-key=" + PRIVATE)
                .run(context -> assertThat(rootMessages(context.getStartupFailure()))
                        .contains("identity-access.security.jwt.public-key"));
    }

    @Test
    void malformedKeysStopStartupWithoutLeakingKeyMaterial() {
        runner.withPropertyValues(
                        "identity-access.security.jwt.private-key=-----BEGIN PRIVATE KEY-----AAAA-----END PRIVATE KEY-----",
                        "identity-access.security.jwt.public-key=" + PUBLIC)
                .run(context -> {
                    String messages = rootMessages(context.getStartupFailure());
                    assertThat(messages)
                            .contains("identity-access.security.jwt.private-key")
                            .doesNotContain("AAAA");
                });
    }

    @Test
    void aMismatchedKeyPairStopsStartup() {
        KeyPair other = RsaKeyFixtures.newKeyPair();
        runner.withPropertyValues(
                        "identity-access.security.jwt.private-key=" + PRIVATE,
                        "identity-access.security.jwt.public-key=" + RsaKeyFixtures.publicPem(other))
                .run(context -> assertThat(rootMessages(context.getStartupFailure()))
                        .contains("do not form a matching key pair"));
    }
}
