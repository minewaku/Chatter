package com.minewaku.chatter.identityaccess.application.port.inbound.shared.response;

public record TokenResponse(String accessToken, String refreshToken) {
}
