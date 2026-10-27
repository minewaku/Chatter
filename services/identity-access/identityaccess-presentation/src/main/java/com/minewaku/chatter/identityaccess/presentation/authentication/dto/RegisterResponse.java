package com.minewaku.chatter.identityaccess.presentation.authentication.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RegisterResponse(
    long userId,
    @Schema(
        description = "False when the account was created but the verification email could not be sent"
            + " right now; ask for a resend later")
        boolean verificationMailSent) {}
