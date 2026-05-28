package com.minewaku.chatter.profile.presentation.messaging.consumer.single.model;

import java.time.Instant;

public record EnablementChangedEventDto(
    Long userId,
    boolean enabled,
    boolean locked,
    boolean deleted,
    Instant deletedAt
) {
    
}
