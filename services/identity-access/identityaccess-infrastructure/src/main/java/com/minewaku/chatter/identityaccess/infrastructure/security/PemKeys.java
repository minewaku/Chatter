package com.minewaku.chatter.identityaccess.infrastructure.security;

import com.minewaku.chatter.identityaccess.infrastructure.security.SigningKeysUnavailableException.Reason;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class PemKeys {

    public static final String PRIVATE_KEY_FIELD = "private-key";
    public static final String PUBLIC_KEY_FIELD = "public-key";

    private PemKeys() {}

    public record RsaKeyPair(RSAPrivateKey privateKey, RSAPublicKey publicKey) {}

    /**
     * Parses a PEM private key (PKCS#8) and a PEM public key (X.509) and checks that they belong together. Failures
     * name the field and the problem, never the input, and do not chain the parser's own cause.
     */
    public static RsaKeyPair parse(String privatePem, String publicPem) {
        var privateKey = parsePrivateKey(privatePem);
        var publicKey = parsePublicKey(publicPem);
        if (!matches(privateKey, publicKey)) {
            throw new SigningKeysUnavailableException(
                    Reason.KEY_PAIR_MISMATCH,
                    PRIVATE_KEY_FIELD + " and " + PUBLIC_KEY_FIELD + " do not form a matching key pair");
        }
        return new RsaKeyPair(privateKey, publicKey);
    }

    static RSAPrivateKey parsePrivateKey(String value) {
        byte[] der = decodePem(value, PRIVATE_KEY_FIELD, "PRIVATE KEY");
        try {
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (GeneralSecurityException | ClassCastException e) {
            throw invalid(PRIVATE_KEY_FIELD, "PKCS#8 RSA private key");
        }
    }

    static RSAPublicKey parsePublicKey(String value) {
        byte[] der = decodePem(value, PUBLIC_KEY_FIELD, "PUBLIC KEY");
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException | ClassCastException e) {
            throw invalid(PUBLIC_KEY_FIELD, "X.509 RSA public key");
        }
    }

    private static boolean matches(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
        if (!(privateKey instanceof RSAPrivateCrtKey crt)) {
            return false;
        }
        return crt.getModulus().equals(publicKey.getModulus())
                && crt.getPublicExponent().equals(publicKey.getPublicExponent());
    }

    private static byte[] decodePem(String value, String field, String label) {
        if (value == null || value.isBlank()) {
            throw invalid(field, label.startsWith("PRIVATE") ? "PKCS#8 RSA private key" : "X.509 RSA public key");
        }
        String body = value.trim()
                .replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s", "");
        if (body.isEmpty()) {
            throw invalid(field, label.startsWith("PRIVATE") ? "PKCS#8 RSA private key" : "X.509 RSA public key");
        }
        try {
            return Base64.getDecoder().decode(body);
        } catch (IllegalArgumentException e) {
            throw invalid(field, label.startsWith("PRIVATE") ? "PKCS#8 RSA private key" : "X.509 RSA public key");
        }
    }

    private static SigningKeysUnavailableException invalid(String field, String expected) {
        return new SigningKeysUnavailableException(Reason.KEY_INVALID, field + " is not a valid " + expected);
    }
}
