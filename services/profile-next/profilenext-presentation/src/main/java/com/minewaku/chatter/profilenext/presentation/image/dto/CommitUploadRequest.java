package com.minewaku.chatter.profilenext.presentation.image.dto;

import jakarta.validation.constraints.NotBlank;

public record CommitUploadRequest(@NotBlank String imageId) {}
