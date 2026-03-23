package com.minewaku.chatter.identityaccess.infrastructure.service.impl;

import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.identityaccess.application.port.outbound.provider.AccessTokenGenerator;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.infrastructure.helper.RsaKeyProvider;

import io.jsonwebtoken.Jwts;

@Service
public class Rs256JwtTokenProvider implements AccessTokenGenerator {

	private static final long ACCESS_TOKEN_EXPIRATION = 1800000; // 10 seconds (in milliseconds)

	private final RsaKeyProvider rsaKeyProvider;

	public Rs256JwtTokenProvider(RsaKeyProvider rsaKeyProvider) {
		this.rsaKeyProvider = rsaKeyProvider;
	}

	@Override
	public String generate(UserId userId, Email email) {
		PrivateKey privateKey = rsaKeyProvider.getPrivateKey();

		Date issuedAt = Date.from(Instant.now());
		Date expiration = Date.from(issuedAt.toInstant().plusMillis(ACCESS_TOKEN_EXPIRATION));

		String jwt = Jwts.builder()
				.claims()
				.subject(userId.getValue().toString())
				.issuer("authentication-service")
				.issuedAt(issuedAt)
				.expiration(expiration)
				.audience().add("chatter").and()
				.add("email", email.getValue())
				// .add("roles", roles.stream()
				// 		.map(role -> role.getCode().getValue())
				// 		.toList())
				.and()
				.signWith(privateKey)
				.compact();

		return jwt;
	}
}
