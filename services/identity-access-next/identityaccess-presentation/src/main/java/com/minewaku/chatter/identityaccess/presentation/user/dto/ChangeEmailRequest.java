package com.minewaku.chatter.identityaccess.presentation.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Core's email rules remain the authority; this only guards the shape. */
public record ChangeEmailRequest(
        @NotBlank @Email @Size(max = 254) String newEmail,
        @NotBlank @Size(max = 128) @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String currentPassword) {

    @Override
    public String toString() {
        return "ChangeEmailRequest";
    }
}
