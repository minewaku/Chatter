package com.minewaku.chatter.profile.start.signing;

import com.minewaku.chatter.profile.infrastructure.security.JwtProperties;
import com.minewaku.chatter.profile.infrastructure.security.PemKeys;
import com.minewaku.chatter.profile.infrastructure.security.SigningKeysUnavailableException;
import com.minewaku.chatter.profile.infrastructure.security.SigningKeysUnavailableException.Reason;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import org.springframework.vault.VaultException;
import org.springframework.vault.core.VaultKeyValueOperationsSupport.KeyValueBackend;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.VaultResponse;
import org.springframework.web.client.ResourceAccessException;

/**
 * Reads the public key from Vault's KV version 2 store, once. Only the {@code public-key} field is requested; this
 * service never asks for the private half. Every failure says where it looked and what went wrong, never what it
 * found.
 */
final class VaultPublicKeyLoader {

    private static final String PREFIX = "The token public key could not be loaded: ";

    private final VaultOperations vault;
    private final JwtProperties.Keys keys;
    private final String endpoint;
    private final String location;

    /** {@code endpoint} is only used in messages, for example {@code http://localhost:5002}. */
    VaultPublicKeyLoader(VaultOperations vault, JwtProperties.Keys keys, String endpoint) {
        this.vault = vault;
        this.keys = keys;
        this.endpoint = endpoint;
        this.location = keys.backend() + "/" + keys.path();
    }

    RSAPublicKey load() {
        Map<String, Object> data = read();
        Object value = data.get(PemKeys.PUBLIC_KEY_FIELD);
        if (value == null || value.toString().isBlank()) {
            throw new SigningKeysUnavailableException(
                    Reason.FIELD_MISSING, PREFIX + location + " has no " + PemKeys.PUBLIC_KEY_FIELD);
        }
        try {
            return PemKeys.parsePublicKey(value.toString());
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
