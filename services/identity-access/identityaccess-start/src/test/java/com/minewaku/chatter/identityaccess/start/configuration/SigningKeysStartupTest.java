package com.minewaku.chatter.identityaccess.start.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.authentication.JwtAccessTokenIssuer;
import com.minewaku.chatter.identityaccess.infrastructure.security.JwtDecoderConfiguration;
import com.minewaku.chatter.identityaccess.infrastructure.security.JwtProperties;
import com.minewaku.chatter.identityaccess.infrastructure.security.PemKeys;
import com.minewaku.chatter.identityaccess.infrastructure.security.SigningKeysUnavailableException;
import com.minewaku.chatter.identityaccess.infrastructure.security.SigningKeysUnavailableException.Reason;
import com.minewaku.chatter.identityaccess.start.support.RsaKeys;
import com.minewaku.chatter.identityaccess.start.support.VaultContainerSupport;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.lang.reflect.Proxy;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.vault.core.VaultOperations;

/** The context-level behavior: the service starts with a usable pair, and otherwise does not start at all. */
class SigningKeysStartupTest {

    @Configuration
    @EnableConfigurationProperties(JwtProperties.class)
    @Import({SigningKeysConfiguration.class, JwtDecoderConfiguration.class})
    static class Config {}

    private final String path = "startup-test/" + UUID.randomUUID();
    private final KeyPair pair = RsaKeys.newKeyPair();

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(Config.class)
                .withPropertyValues("identity-access.security.jwt.keys.path=" + path);
    }

    private void seed(KeyPair keys) {
        VaultContainerSupport.put(
                path, Map.of("private-key", RsaKeys.privatePem(keys), "public-key", RsaKeys.publicPem(keys)));
    }

    private static Reason reasonOf(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof SigningKeysUnavailableException e) {
                return e.reason();
            }
        }
        throw new AssertionError("no SigningKeysUnavailableException in the cause chain of " + failure);
    }

    @Test
    void withoutAVaultClientTheContextDoesNotStart() {
        runner().run(context ->
                assertThat(reasonOf(context.getStartupFailure())).isEqualTo(Reason.VAULT_DISABLED));
    }

    @Test
    void withASeededPairTheContextStartsAndVerifiesWhatItIssues() {
        seed(pair);

        runner().withBean(VaultOperations.class, () -> VaultContainerSupport.template(VaultContainerSupport.ROOT_TOKEN))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var keys = context.getBean(PemKeys.RsaKeyPair.class);
                    String token = new JwtAccessTokenIssuer(
                                    keys, new JwtProperties(null, null, Duration.ofMinutes(5), null), Clock.systemUTC())
                            .issue(new UserId(1L), new Email("alice@example.com"), "session-1");
                    assertThat(context.getBean(JwtDecoder.class).decode(token).getSubject())
                            .isEqualTo("1");
                });
    }

    @Test
    void afterAFailedStartCorrectingTheCauseIsTheOnlyChangeNeeded() {
        var vault = (java.util.function.Supplier<VaultOperations>)
                () -> VaultContainerSupport.template(VaultContainerSupport.ROOT_TOKEN);

        runner().withBean(VaultOperations.class, vault).run(context -> assertThat(reasonOf(context.getStartupFailure()))
                .isEqualTo(Reason.SECRET_NOT_FOUND));

        seed(pair);

        runner().withBean(VaultOperations.class, vault)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void leftoverLocalKeysAreNeverAFallbackWhenTheStoreIsUnreachable() {
        KeyPair other = RsaKeys.newKeyPair();

        runner().withBean(
                        VaultOperations.class,
                        () -> VaultContainerSupport.template(
                                VaultContainerSupport.host(),
                                VaultContainerSupport.closedPort(),
                                VaultContainerSupport.ROOT_TOKEN))
                .withPropertyValues(
                        "identity-access.security.jwt.private-key=" + RsaKeys.privatePem(other),
                        "identity-access.security.jwt.public-key=" + RsaKeys.publicPem(other))
                .run(context ->
                        assertThat(reasonOf(context.getStartupFailure())).isEqualTo(Reason.STORE_UNREACHABLE));
    }

    @Test
    void leftoverLocalKeysDoNotChangeWhichKeysAreUsed() {
        seed(pair);
        KeyPair other = RsaKeys.newKeyPair();

        runner().withBean(VaultOperations.class, () -> VaultContainerSupport.template(VaultContainerSupport.ROOT_TOKEN))
                .withPropertyValues(
                        "identity-access.security.jwt.private-key=" + RsaKeys.privatePem(other),
                        "identity-access.security.jwt.public-key=" + RsaKeys.publicPem(other))
                .run(context -> assertThat(context.getBean(PemKeys.RsaKeyPair.class)
                                .publicKey()
                                .getEncoded())
                        .isEqualTo(pair.getPublic().getEncoded()));
    }

    @Test
    void aBlankVaultTokenIsReportedAsNoCredential() {
        runner().withBean(VaultOperations.class, () -> VaultContainerSupport.template(VaultContainerSupport.ROOT_TOKEN))
                .withPropertyValues("spring.cloud.vault.token=")
                .run(context -> {
                    assertThat(reasonOf(context.getStartupFailure())).isEqualTo(Reason.CREDENTIAL_REJECTED);
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("VAULT_TOKEN");
                });
    }

    @Test
    void theServiceKeepsWorkingWhenVaultGoesAwayAfterStartup() {
        seed(pair);
        AtomicBoolean down = new AtomicBoolean();
        VaultOperations real = VaultContainerSupport.template(VaultContainerSupport.ROOT_TOKEN);
        VaultOperations switchable = (VaultOperations) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {VaultOperations.class}, (proxy, method, args) -> {
                    if (down.get()) {
                        throw new IllegalStateException("Vault went away");
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });

        runner().withBean(VaultOperations.class, () -> switchable).run(context -> {
            assertThat(context).hasNotFailed();
            down.set(true);
            assertThatThrownBy(() -> switchable.opsForKeyValue("secret", null)).hasMessage("Vault went away");

            String token = new JwtAccessTokenIssuer(
                            context.getBean(PemKeys.RsaKeyPair.class),
                            new JwtProperties(null, null, Duration.ofMinutes(5), null),
                            Clock.systemUTC())
                    .issue(new UserId(2L), new Email("bob@example.com"), "session-2");
            assertThat(context.getBean(JwtDecoder.class).decode(token).getSubject())
                    .isEqualTo("2");
        });
    }
}
