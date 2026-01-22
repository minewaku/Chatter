package com.minewaku.chatter.domain.response;

import com.minewaku.chatter.domain.model.RefreshToken;


public record TokenResponse(String accessToken, RefreshToken refreshToken) {
}
