package com.minewaku.chatter.identityaccess.presentation.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank String email, @NotBlank String username, @NotNull LocalDate birthday, @NotBlank String password) {}
