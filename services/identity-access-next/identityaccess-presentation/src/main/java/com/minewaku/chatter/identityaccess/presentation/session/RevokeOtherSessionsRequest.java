package com.minewaku.chatter.identityaccess.presentation.session;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeOtherSessionsRequest(
        @NotBlank @Size(max = 128) @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String currentPassword) {

    @Override
    public String toString() {
        return "RevokeOtherSessionsRequest";
    }
}
