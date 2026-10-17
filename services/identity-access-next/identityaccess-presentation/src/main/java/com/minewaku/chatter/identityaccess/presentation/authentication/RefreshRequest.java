package com.minewaku.chatter.identityaccess.presentation.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** An absent or blank credential is passed on so core treats it as an invalid one (generic {@code 401}). */
public record RefreshRequest(@Size(max = 512) @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String refreshToken) {

    @Override
    public String toString() {
        return "RefreshRequest";
    }
}
