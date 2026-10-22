package com.minewaku.chatter.identityaccess.start.configuration;

import com.minewaku.chatter.identityaccess.infrastructure.security.JwtProperties;
import com.minewaku.chatter.identityaccess.infrastructure.security.PemKeys;
import com.minewaku.chatter.identityaccess.infrastructure.security.SigningKeysUnavailableException;
import com.minewaku.chatter.identityaccess.infrastructure.security.SigningKeysUnavailableException.Reason;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.vault.core.VaultOperations;

/**
 * The one place the signing key pair comes from: Vault, read once while the context is built. There is no other
 * source and no fallback; any problem stops start-up with a {@link SigningKeysUnavailableException}.
 */
@Configuration
class SigningKeysConfiguration {

    @Bean
    PemKeys.RsaKeyPair signingKeys(
            ObjectProvider<VaultOperations> vault, JwtProperties properties, Environment environment) {
        // Checked before asking for the Vault client: with no token its construction fails with a message that does
        // not name the credential.
        if (environment.containsProperty("spring.cloud.vault.token")
                && environment.getProperty("spring.cloud.vault.token", "").isBlank()) {
            throw new SigningKeysUnavailableException(
                    Reason.CREDENTIAL_REJECTED,
                    "Signing keys could not be loaded: no Vault credential is configured (set VAULT_TOKEN)");
        }
        VaultOperations operations = vault.getIfAvailable();
        if (operations == null) {
            throw new SigningKeysUnavailableException(
                    Reason.VAULT_DISABLED, "Signing keys can only be loaded from Vault, but Vault support is disabled");
        }
        return new VaultSigningKeyLoader(operations, properties.keys(), endpoint(environment)).load();
    }

    private static String endpoint(Environment environment) {
        return environment.getProperty("spring.cloud.vault.scheme", "http")
                + "://"
                + environment.getProperty("spring.cloud.vault.host", "localhost")
                + ":"
                + environment.getProperty("spring.cloud.vault.port", "8200");
    }
}
