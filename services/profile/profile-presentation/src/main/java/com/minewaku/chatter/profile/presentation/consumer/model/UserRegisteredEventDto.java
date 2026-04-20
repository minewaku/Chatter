package com.minewaku.chatter.profile.presentation.consumer.model;

import java.time.Instant;
import java.time.LocalDate;

public record UserRegisteredEventDto(
    Long userId,
    String email,
    String username,
    LocalDate birthday,
    boolean enabled,
    boolean deleted,
    boolean locked,
    Instant deletedAt
) {}