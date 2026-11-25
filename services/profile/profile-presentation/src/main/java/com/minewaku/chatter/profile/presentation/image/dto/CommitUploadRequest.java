package com.minewaku.chatter.profile.presentation.image.dto;

import jakarta.validation.constraints.NotBlank;

public record CommitUploadRequest(@NotBlank String imageId) {}
