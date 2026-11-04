package com.minewaku.chatter.profilenext.infrastructure.security;

import com.minewaku.chatter.profilenext.infrastructure.security.SigningKeysUnavailableException.Reason;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Parses the one key this service holds: the public half of the platform's token signing pair. */
public final class PemKeys {

    public static final String PUBLIC_KEY_FIELD = "public-key";

    private PemKeys() {}

    /** The public key access tokens are checked against; supplied as a bean by the application module. */
    public record PublicKeyHolder(RSAPublicKey publicKey) {}

    /**
     * Parses a PEM public key (X.509). Failures name the field and the problem, never the input, and do not chain the
     * parser's own cause.
     */
    public static RSAPublicKey parsePublicKey(String pem) {
        byte[] der = decodePem(pem);
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException | ClassCastException e) {
            throw invalid();
        }
    }

    private static byte[] decodePem(String value) {
        if (value == null || value.isBlank()) {
            throw invalid();
        }
        String body = value.trim()
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        if (body.isEmpty()) {
            throw invalid();
        }
        try {
            return Base64.getDecoder().decode(body);
        } catch (IllegalArgumentException e) {
            throw invalid();
        }
    }

    private static SigningKeysUnavailableException invalid() {
        return new SigningKeysUnavailableException(
                Reason.KEY_INVALID, PUBLIC_KEY_FIELD + " is not a valid X.509 RSA public key");
    }
}
