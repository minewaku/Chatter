package com.minewaku.chatter.identityaccess.start.configuration;

import com.minewaku.chatter.identityaccess.infrastructure.security.JwtProperties;
import com.minewaku.chatter.identityaccess.infrastructure.security.PemKeys;
import com.minewaku.chatter.identityaccess.infrastructure.security.SigningKeysUnavailableException;
import com.minewaku.chatter.identityaccess.infrastructure.security.SigningKeysUnavailableException.Reason;
import java.util.Map;
import org.springframework.vault.VaultException;
import org.springframework.vault.core.VaultKeyValueOperationsSupport.KeyValueBackend;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.VaultResponse;
import org.springframework.web.client.ResourceAccessException;

/**
 * Reads the signing key pair from Vault's KV version 2 store, once. Every failure says where it looked and what went
 * wrong, never what it found: no message holds key text or a fragment of it.
 */
final class VaultSigningKeyLoader {

    private static final String PREFIX = "Signing keys could not be loaded: ";

    private final VaultOperations vault;
    private final JwtProperties.Keys keys;
    private final String endpoint;
    private final String location;

    /** {@code endpoint} is only used in messages, for example {@code http://localhost:5002}. */
    VaultSigningKeyLoader(VaultOperations vault, JwtProperties.Keys keys, String endpoint) {
        this.vault = vault;
        this.keys = keys;
        this.endpoint = endpoint;
        this.location = keys.backend() + "/" + keys.path();
    }

    PemKeys.RsaKeyPair load() {
        Map<String, Object> data = read();
        String privatePem = field(data, PemKeys.PRIVATE_KEY_FIELD);
        String publicPem = field(data, PemKeys.PUBLIC_KEY_FIELD);
        try {
            return PemKeys.parse(privatePem, publicPem);
        } catch (SigningKeysUnavailableException e) {
            throw new SigningKeysUnavailableException(e.reason(), PREFIX + e.getMessage() + " (" + location + ")");
        }
    }

    private Map<String, Object> read() {
        VaultResponse response;
        try {
            response =
                    vault.opsForKeyValue(keys.backend(), KeyValueBackend.KV_2).get(keys.path());
        } catch (ResourceAccessException e) {
            throw new SigningKeysUnavailableException(
                    Reason.STORE_UNREACHABLE, PREFIX + "Vault is not reachable at " + endpoint, e);
        } catch (VaultException e) {
            if (isForbidden(e)) {
                throw new SigningKeysUnavailableException(
                        Reason.CREDENTIAL_REJECTED,
                        PREFIX + "Vault rejected the service credential for " + location,
                        e);
            }
            throw new SigningKeysUnavailableException(
                    Reason.STORE_UNREACHABLE,
                    PREFIX + "Vault did not answer a read of " + location + " at " + endpoint,
                    e);
        } catch (RuntimeException e) {
            if (isConnectionFailure(e)) {
                throw new SigningKeysUnavailableException(
                        Reason.STORE_UNREACHABLE, PREFIX + "Vault is not reachable at " + endpoint, e);
            }
            throw e;
        }
        if (response == null || response.getData() == null) {
            throw new SigningKeysUnavailableException(Reason.SECRET_NOT_FOUND, PREFIX + "no secret at " + location);
        }
        return response.getData();
    }

    private String field(Map<String, Object> data, String name) {
        Object value = data.get(name);
        if (value == null || value.toString().isBlank()) {
            throw new SigningKeysUnavailableException(Reason.FIELD_MISSING, PREFIX + location + " has no " + name);
        }
        return value.toString();
    }

    private static boolean isForbidden(VaultException e) {
        String message = e.getMessage();
        return message != null && (message.contains("Status 403") || message.contains("permission denied"));
    }

    private static boolean isConnectionFailure(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof java.io.IOException) {
                return true;
            }
        }
        return false;
    }
}
