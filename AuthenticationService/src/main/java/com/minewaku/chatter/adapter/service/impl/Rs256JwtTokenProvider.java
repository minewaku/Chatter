package com.minewaku.chatter.adapter.service.impl;

import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.adapter.helper.RsaKeyProvider;
import com.minewaku.chatter.domain.model.Role;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.out.service.AccessTokenGenerator;

import io.jsonwebtoken.Jwts;

@Service
public class Rs256JwtTokenProvider implements AccessTokenGenerator {

	private static final long ACCESS_TOKEN_EXPIRATION = 1800000; // 10 seconds (in milliseconds)

	private final RsaKeyProvider rsaKeyProvider;

	public Rs256JwtTokenProvider(RsaKeyProvider rsaKeyProvider) {
		this.rsaKeyProvider = rsaKeyProvider;
	}

	@Override
	public String generate(User user, Set<Role> roles) {
		PrivateKey privateKey = rsaKeyProvider.getPrivateKey();

		Date issuedAt = Date.from(Instant.now());
		Date expiration = Date.from(issuedAt.toInstant().plusMillis(ACCESS_TOKEN_EXPIRATION));

		String jwt = Jwts.builder()
				.claims()
				.subject(user.getId().getValue().toString())
				.issuer("authentication-service")
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
	}
}
