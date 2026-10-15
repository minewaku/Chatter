package com.minewaku.chatter.identityaccess.infrastructure.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.core.io.DefaultResourceLoader;

public final class PemKeys {

    static final String PRIVATE_KEY_PROPERTY = "identity-access.security.jwt.private-key";
    static final String PUBLIC_KEY_PROPERTY = "identity-access.security.jwt.public-key";

    private PemKeys() {}

    public record RsaKeyPair(RSAPrivateKey privateKey, RSAPublicKey publicKey) {}

    public static RsaKeyPair load(JwtProperties properties) {
        var privateKey = loadPrivateKey(properties.privateKey());
        var publicKey = loadPublicKey(properties.publicKey());
        if (!matches(privateKey, publicKey)) {
            throw new IllegalStateException(
                    PRIVATE_KEY_PROPERTY + " and " + PUBLIC_KEY_PROPERTY + " do not form a matching key pair");
        }
        return new RsaKeyPair(privateKey, publicKey);
    }

    static RSAPrivateKey loadPrivateKey(String value) {
        byte[] der = decodePem(value, PRIVATE_KEY_PROPERTY, "PRIVATE KEY");
        try {
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (GeneralSecurityException | ClassCastException e) {
            throw new IllegalStateException(PRIVATE_KEY_PROPERTY + " is not a valid PKCS#8 RSA private key", e);
        }
    }

    static RSAPublicKey loadPublicKey(String value) {
        byte[] der = decodePem(value, PUBLIC_KEY_PROPERTY, "PUBLIC KEY");
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException | ClassCastException e) {
            throw new IllegalStateException(PUBLIC_KEY_PROPERTY + " is not a valid X.509 RSA public key", e);
        }
    }

    private static boolean matches(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
        if (!(privateKey instanceof RSAPrivateCrtKey crt)) {
            return false;
        }
        return crt.getModulus().equals(publicKey.getModulus())
                && crt.getPublicExponent().equals(publicKey.getPublicExponent());
    }

    private static byte[] decodePem(String value, String property, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(property + " is missing");
        }
        String text = value.trim();
        if (text.startsWith("file:") || text.startsWith("classpath:")) {
            try {
                text = new DefaultResourceLoader().getResource(text).getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException(property + " could not be read from its location", e);
            }
        }
        String body = text.replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s", "");
        if (body.isEmpty()) {
            throw new IllegalStateException(property + " does not contain a PEM " + label);
        }
        try {
            return Base64.getDecoder().decode(body);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(property + " is not valid Base64 PEM text", e);
        }
    }
}
