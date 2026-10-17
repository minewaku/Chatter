package com.minewaku.chatter.identityaccess.presentation.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** An absent or blank credential revokes nothing and still gets {@code 204}. */
public record LogoutRequest(@Size(max = 512) @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String refreshToken) {

    @Override
    public String toString() {
        return "LogoutRequest";
    }
}
