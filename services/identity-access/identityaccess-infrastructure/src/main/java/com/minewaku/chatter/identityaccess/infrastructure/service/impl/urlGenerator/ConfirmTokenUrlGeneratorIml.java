package com.minewaku.chatter.identityaccess.infrastructure.service.impl.urlGenerator;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.identityaccess.application.port.outbound.provider.UrlGenerator;

@Service
public class ConfirmTokenUrlGeneratorIml implements UrlGenerator.ConfirmationTokenUrlGenerator {

    @Override
    public String generate(String token) {
        return "http://localhost:5001/api/v1/auth/verification/confirm?token=" + token;
    }
    
}
