package com.minewaku.chatter.identityaccess.presentation.authentication;

import com.minewaku.chatter.identityaccess.presentation.shared.validation.PasswordRule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Registration input. Core value objects remain the authority on the real rules. */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 1, max = 64) String username,
        @NotNull @Past LocalDate birthday,
        @NotBlank @Size(max = 128) @Pattern(regexp = PasswordRule.REGEX, message = PasswordRule.MESSAGE) @Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
                String password) {

    @Override
    public String toString() {
        return "RegisterRequest";
    }
}
