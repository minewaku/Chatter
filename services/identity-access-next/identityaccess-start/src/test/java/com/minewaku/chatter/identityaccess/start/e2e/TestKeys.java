package com.minewaku.chatter.identityaccess.start.e2e;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/** A throwaway RSA key pair for the signing and validation configuration of the end-to-end context. */
final class TestKeys {

    private static final KeyPair PAIR = generate();

    private TestKeys() {}

    static String privatePem() {
        return pem("PRIVATE KEY", PAIR.getPrivate().getEncoded());
    }

    static String publicPem() {
        return pem("PUBLIC KEY", PAIR.getPublic().getEncoded());
    }

    /** A token signed with the service's test key but lacking the session claim, as issued before session binding. */
    static String legacyAccessToken(long userId, String email) {
        java.time.Instant now = java.time.Instant.now();
        return signed(PAIR, "identity-access", "chatter", Long.toString(userId), null, now, now.plusSeconds(900));
    }

    /** A well-formed, correctly shaped token signed with a key the service does not trust. */
    static String foreignKeyToken(long userId, String sessionId) {
        java.time.Instant now = java.time.Instant.now();
        return signed(
                generate(), "identity-access", "chatter", Long.toString(userId), sessionId, now, now.plusSeconds(900));
    }

    /** A token signed with the service's key whose lifetime has already ended. */
    static String expiredToken(long userId, String sessionId) {
        java.time.Instant past = java.time.Instant.now().minusSeconds(7200);
        return signed(
                PAIR, "identity-access", "chatter", Long.toString(userId), sessionId, past, past.plusSeconds(900));
    }

    /** A token signed with the service's key but naming another issuer. */
    static String wrongIssuerToken(long userId, String sessionId) {
        java.time.Instant now = java.time.Instant.now();
        return signed(PAIR, "someone-else", "chatter", Long.toString(userId), sessionId, now, now.plusSeconds(900));
    }

    private static String signed(
            KeyPair pair,
            String issuer,
            String audience,
            String subject,
            String sessionId,
            java.time.Instant issuedAt,
            java.time.Instant expiresAt) {
        com.nimbusds.jose.jwk.RSAKey jwk = new com.nimbusds.jose.jwk.RSAKey.Builder(
                        (java.security.interfaces.RSAPublicKey) pair.getPublic())
                .privateKey(pair.getPrivate())
                .build();
        org.springframework.security.oauth2.jwt.JwtClaimsSet.Builder claims =
                org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                        .issuer(issuer)
                        .audience(java.util.List.of(audience))
                        .subject(subject)
                        .issuedAt(issuedAt)
                        .expiresAt(expiresAt);
        if (sessionId != null) {
            claims.claim("sid", sessionId);
        }
        return new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(
                        new com.nimbusds.jose.jwk.source.ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(jwk)))
                .encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                        org.springframework.security.oauth2.jwt.JwsHeader.with(
                                        org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                                .build(),
                        claims.build()))
                .getTokenValue();
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String pem(String label, byte[] der) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return "-----BEGIN " + label + "-----\n" + body + "\n-----END " + label + "-----\n";
    }
}
