package com.minewaku.chatter.service.impl;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import com.minewaku.chatter.config.properties.JwtProperties;
import com.minewaku.chatter.service.IJwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Service
public class JwtService implements IJwtService {

	// Các claim chuẩn JWT cần loại bỏ
	private static final Set<String> STANDARD_JWT_CLAIMS = Set.of(
			"iss", "sub", "aud", "exp", "nbf", "iat", "jti");

	private static final String ROLES_CLAIM = "roles";

	private final JwtProperties jwtProperties;

	public JwtService(JwtProperties jwtProperties) {
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

	public Map<String, Object> getCustomClaims(Jwt jwt) {
		Map<String, Object> allClaims = jwt.getClaims();

		allClaims.keySet().removeAll(STANDARD_JWT_CLAIMS);
		allClaims.remove(ROLES_CLAIM);

		return allClaims;
	}

	// private PrivateKey getSignInKey() throws NoSuchAlgorithmException,
	// InvalidKeySpecException {
	// String pem = jwtProperties.getPrivateKey()
	// .replace("-----BEGIN PRIVATE KEY-----", "")
	// .replace("-----END PRIVATE KEY-----", "")
	// .replaceAll("\\s", "");

	// byte[] keyBytes = Base64.getDecoder().decode(pem);
	// PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
	// PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);

	// return privateKey;
	// }

	private PublicKey getVerifyKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		String pem = jwtProperties.getPublicKey()
				.replace("-----BEGIN PUBLIC KEY-----", "")
				.replace("-----END PUBLIC KEY-----", "")
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
