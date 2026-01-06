package com.minewaku.chatter.adapter.service.impl;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import com.minewaku.chatter.adapter.helper.RsaKeyProvider;
import com.minewaku.chatter.adapter.service.IAccessTokenVerifier;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;

@Service
public class AccessTokenVerify implements IAccessTokenVerifier {
    
	private final RsaKeyProvider rsaKeyProvider;

	public AccessTokenVerify(RsaKeyProvider rsaKeyProvider) {
		this.rsaKeyProvider = rsaKeyProvider;
	}

    @Override
    public Claims extractClaims(String token) {
		try {
			PublicKey publicKey = rsaKeyProvider.getPublicKey();

			return Jwts.parser()
					.verifyWith(publicKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch (MalformedJwtException e) {
			throw e;
		}
	}

    @Override
	public JwtDecoder getJwtDecoder() {
        RSAPublicKey publicKey = (RSAPublicKey) rsaKeyProvider.getPublicKey();
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
	}
}
