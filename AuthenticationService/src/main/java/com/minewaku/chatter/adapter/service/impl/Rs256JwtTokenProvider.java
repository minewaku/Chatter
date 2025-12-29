package com.minewaku.chatter.adapter.service.impl;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import com.minewaku.chatter.adapter.config.properties.VaultJwtProperties;
import com.minewaku.chatter.adapter.exception.ApiException;
import com.minewaku.chatter.domain.model.Role;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.out.service.AccessTokenGenerator;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;

@Service
public class Rs256JwtTokenProvider implements AccessTokenGenerator {

	private static final long ACCESS_TOKEN_EXPIRATION = 1800000; // 10 seconds (in milliseconds)

	private final VaultJwtProperties jwtProperties;

	public Rs256JwtTokenProvider(VaultJwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
	}

	public Claims extractClaims(String token) {
		try {
			PublicKey publicKey = getVerifyKey();

			return Jwts.parser()
					.verifyWith(publicKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();

		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new ApiException("Internal error", "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, e);
		} catch (MalformedJwtException e) {
			throw e;
		}
	}

	@Override
	public String generate(User user, Set<Role> roles) {
		try {
			PrivateKey privateKey = getSignInKey();

			Date issuedAt = Date.from(Instant.now());
			Date expiration = Date.from(issuedAt.toInstant().plusMillis(ACCESS_TOKEN_EXPIRATION));

			String jwt = Jwts.builder()
					.claims()
					.subject(user.getId().getValue().toString())
					.issuer("chatter-idp")
					.issuedAt(issuedAt)
					.expiration(expiration)
					.audience().add("chatter").and()
					.add("email", user.getEmail().getValue())
					.add("roles", roles.stream()
							.map(role -> role.getCode().getValue())
							.toList())
					.and()
					.signWith(privateKey)
					.compact();

			return jwt;

		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException("Cannot create JWT for user " + user.getId(), e);
		}
	}

	private PrivateKey getSignInKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		String pem = jwtProperties.getPrivateKey()
				.replaceAll("-+BEGIN.*KEY-+", "")
				.replaceAll("-+END.*KEY-+", "")
				.replaceAll("\\s", "");

		byte[] keyBytes = Base64.getDecoder().decode(pem);
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);

		return privateKey;
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

	public JwtDecoder getJwtDecoder() {
		try {
			RSAPublicKey publicKey = (RSAPublicKey) getVerifyKey();
			return NimbusJwtDecoder.withPublicKey(publicKey).build();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException("Failed to create JwtDecoder", e);
		}
	}

}
