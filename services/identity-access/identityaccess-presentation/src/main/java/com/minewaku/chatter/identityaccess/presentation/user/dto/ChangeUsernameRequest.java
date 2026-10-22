package com.minewaku.chatter.identityaccess.presentation.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Core's username rules remain the authority; this only guards the shape. */
public record ChangeUsernameRequest(@NotBlank @Size(min = 1, max = 64) String username) {

    @Override
    public String toString() {
        return "ChangeUsernameRequest";
    }
}
