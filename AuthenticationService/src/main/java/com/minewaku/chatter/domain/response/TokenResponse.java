package com.minewaku.chatter.domain.response;

import com.minewaku.chatter.domain.model.RefreshToken;

public class TokenResponse {
	private final String accessToken;
	private final RefreshToken refreshToken;
	
	public TokenResponse(String accessToken, RefreshToken refreshToken) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
	}
	
	public String accessToken() {
		return this.accessToken;
	}
	
	public RefreshToken refreshToken() {
		return this.refreshToken;
	}
}
