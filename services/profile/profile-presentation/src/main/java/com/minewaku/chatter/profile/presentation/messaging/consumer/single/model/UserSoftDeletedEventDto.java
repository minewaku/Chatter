package com.minewaku.chatter.profile.presentation.messaging.consumer.single.model;

import java.time.Instant;
import java.time.LocalDate;

public record UserSoftDeletedEventDto(
    Long userId,
    String email,
    String username,
    LocalDate birthday,
    boolean enabled,
    boolean locked,
    boolean deleted,
    Instant deletedAt
) {
}
