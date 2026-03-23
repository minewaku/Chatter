package com.minewaku.chatter.identityaccess.application.port.inbound.command.shared.response;

public record TokenResponse(String accessToken, String refreshToken) {
}
