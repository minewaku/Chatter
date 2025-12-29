package com.minewaku.chatter.service.impl;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import com.minewaku.chatter.config.properties.VaultJwtProperties;
import com.minewaku.chatter.service.IJwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Service
public class JwtService implements IJwtService {
	private final VaultJwtProperties jwtProperties;

	public JwtService(VaultJwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
	}

	@Override
	public Claims extractClaims(String token) {
		try {
			PublicKey publicKey = getVerifyKey();

			Claims claims = Jwts.parser()
					.verifyWith(publicKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();

			return claims;
		} catch (Exception e) {
			throw new RuntimeException("Invalid JWT token", e);
		}
	}

	private PublicKey getVerifyKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		String pem = jwtProperties.getPublicKey()
				.replaceAll("-+BEGIN.*KEY-+", "")
				.replaceAll("-+END.*KEY-+", "")
				.replaceAll("\\s", "");

		byte[] keyBytes = Base64.getDecoder().decode(pem);
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		return KeyFactory.getInstance("RSA").generatePublic(spec);
	}

	@Override
	public JwtDecoder getJwtDecoder() {
		try {
			RSAPublicKey publicKey = (RSAPublicKey) getVerifyKey();
			return NimbusJwtDecoder.withPublicKey(publicKey).build();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException("Failed to create JwtDecoder", e);
		}
	}
}
