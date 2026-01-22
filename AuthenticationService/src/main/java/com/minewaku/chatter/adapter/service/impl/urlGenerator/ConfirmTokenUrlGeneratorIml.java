package com.minewaku.chatter.adapter.service.impl.urlGenerator;

import com.minewaku.chatter.domain.port.out.service.UrlGenerator;

public class ConfirmTokenUrlGeneratorIml implements UrlGenerator.ConfirmTokenUrlGenerator {

    @Override
    public String generate(String token) {
        return "http://localhost:5001/api/v1/auth/verification/confirm?token=" + token;
    }
    
}
