package com.minewaku.chatter.message.infrastructure.service.impl;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.message.application.port.outbound.service.InviteCodeGenerator;

@Component
public class ReadableInviteCodeGenerator implements InviteCodeGenerator {

    // Loại bỏ các ký tự dễ nhầm lẫn như O, 0, I, 1, L
    private static final String CHARACTERS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(randomIndex));
        }
        return code.toString();
    }
}