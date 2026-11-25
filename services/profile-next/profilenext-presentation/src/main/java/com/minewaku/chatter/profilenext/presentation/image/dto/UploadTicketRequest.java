package com.minewaku.chatter.profilenext.presentation.image.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** What the client is about to upload. Whether the type and size are allowed for the slot is the policy's call. */
public record UploadTicketRequest(@NotBlank String contentType, @NotNull @Positive Long sizeBytes) {}
