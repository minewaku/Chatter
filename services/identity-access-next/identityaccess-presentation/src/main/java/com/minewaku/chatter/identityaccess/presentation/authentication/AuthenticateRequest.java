package com.minewaku.chatter.identityaccess.presentation.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthenticateRequest(
        @NotBlank @Size(max = 254) String email,
        @NotBlank @Size(max = 128) @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String password) {

    @Override
    public String toString() {
        return "AuthenticateRequest";
    }
}
