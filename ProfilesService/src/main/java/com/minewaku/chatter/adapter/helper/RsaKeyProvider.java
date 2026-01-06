package com.minewaku.chatter.adapter.helper;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.adapter.config.properties.VaultJwtProperties;

import lombok.Getter;

@Component
public class RsaKeyProvider {

    private final VaultJwtProperties jwtProperties;

    @Getter
    private final RSAPrivateKey privateKey;

    @Getter
    private final RSAPublicKey publicKey;

    public RsaKeyProvider(VaultJwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.privateKey = loadPrivateKey(this.jwtProperties.getPrivateKey());
        this.publicKey = loadPublicKey(this.jwtProperties.getPublicKey());
    }

    private RSAPrivateKey loadPrivateKey(String pem) {
        try {
            String sanitizedPem = pem
                    .replaceAll("-+BEGIN.*KEY-+", "")
                    .replaceAll("-+END.*KEY-+", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(sanitizedPem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException("Failed to load Private Key", e);
		}
    }

    private RSAPublicKey loadPublicKey(String pem) {
        try {
            String sanitizedPem = pem
                    .replaceAll("-+BEGIN.*KEY-+", "")
                    .replaceAll("-+END.*KEY-+", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(sanitizedPem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException("Failed to load Public Key", e);
		}
    }
}

