package com.minewaku.chatter.identityaccess.application.port.outbound.provider;

import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.Session;

public interface RefreshTokenEncryptor {
    String encrypt(Session session);
    TokenPayload decrypt(String refreshToken); 

    public record TokenPayload(String sessionId, int generation) {}
}
