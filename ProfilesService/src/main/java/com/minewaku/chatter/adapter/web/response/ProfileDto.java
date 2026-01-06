package com.minewaku.chatter.adapter.web.response;

import java.time.Instant;
import java.time.LocalDate;

import lombok.NonNull;

public record ProfileDto(
    @NonNull Long id,
    @NonNull String username,
    String displayName,
    String bio,
    String avatar,
    String banner,
    @NonNull LocalDate birthday,
    @NonNull Instant createdAt
){}
