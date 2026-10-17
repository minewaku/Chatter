package com.minewaku.chatter.identityaccess.presentation.user;

import com.minewaku.chatter.identityaccess.presentation.shared.validation.PasswordRule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(max = 128) @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String currentPassword,
        @NotBlank @Size(max = 128) @Pattern(regexp = PasswordRule.REGEX, message = PasswordRule.MESSAGE) @Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
                String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest";
    }
}
