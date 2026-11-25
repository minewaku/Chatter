package com.minewaku.chatter.profile.start.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

/** A throwaway RSA key pair and a signer that issues tokens the way identity-access does. */
public final class RsaKeyFixtures {

    public static final String ISSUER = "identity-access";

    private final KeyPair pair;

    public RsaKeyFixtures() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.pair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public RSAPublicKey publicKey() {
        return (RSAPublicKey) pair.getPublic();
    }

    /** A signed token for {@code userId}, with an {@code sid} claim, that expires at {@code expiresAt}. */
    public String token(long userId, Instant expiresAt) {
        return token(Long.toString(userId), ISSUER, expiresAt);
    }

    public String token(String subject, String issuer, Instant expiresAt) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(subject)
                .claim("sid", "session-1")
                .issueTime(Date.from(Instant.now().minusSeconds(5)))
                .expirationTime(Date.from(expiresAt))
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner((RSAPrivateKey) pair.getPrivate()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
