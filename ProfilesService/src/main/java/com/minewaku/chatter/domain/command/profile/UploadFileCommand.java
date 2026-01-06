package com.minewaku.chatter.domain.command.profile;

import com.minewaku.chatter.domain.value.InputImage;
import com.minewaku.chatter.domain.value.id.UserId;

import jakarta.validation.constraints.NotNull;

public record UploadFileCommand(@NotNull UserId userId, @NotNull InputImage inputImage) {
    
}
