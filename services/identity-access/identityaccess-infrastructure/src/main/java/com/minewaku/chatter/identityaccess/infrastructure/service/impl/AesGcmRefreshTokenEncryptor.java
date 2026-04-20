package com.minewaku.chatter.identityaccess.infrastructure.service.impl;

import java.util.Base64;

import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.RefreshTokenEncryptor;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.exception.InvalidRefreshTokenException;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.Session;
import com.minewaku.chatter.identityaccess.infrastructure.config.property.VaultRefreshProperties;

@Service
public class AesGcmRefreshTokenEncryptor implements RefreshTokenEncryptor {

    private final BytesEncryptor encryptor;
    private final ObjectMapper objectMapper;

    public AesGcmRefreshTokenEncryptor(
            VaultRefreshProperties vaultRefreshProperties,
            ObjectMapper objectMapper) {
                
        this.encryptor = Encryptors.stronger(
                vaultRefreshProperties.getPassword(),
                vaultRefreshProperties.getSalt()
        );
        this.objectMapper = objectMapper;
    }

    @Override
    public String encrypt(Session session) {
        try {
            TokenPayload payload = new TokenPayload(session.getSessionId().getValue().toString(), session.getGeneration());
            byte[] jsonBytes = objectMapper.writeValueAsBytes(payload);
            byte[] encryptedBytes = encryptor.encrypt(jsonBytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(encryptedBytes);
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("System error: Unable to serialize session payload to JSON", e);
        }
    }

    @Override
    public TokenPayload decrypt(String refreshToken) {
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(refreshToken);
            byte[] decryptedBytes = encryptor.decrypt(decodedBytes);
            return objectMapper.readValue(decryptedBytes, TokenPayload.class);
            
        } catch (IllegalArgumentException e) {
            throw new InvalidRefreshTokenException("Invalid refresh token format");
        } catch (Exception e) {
            throw new RuntimeException("Invalid refresh token, the token has been tampered with or is corrupted", e);
        }
    }
}