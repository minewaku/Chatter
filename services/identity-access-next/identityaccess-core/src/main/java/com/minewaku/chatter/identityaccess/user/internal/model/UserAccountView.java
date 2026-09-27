package com.minewaku.chatter.identityaccess.user.internal.model;

import java.time.Instant;
import java.time.LocalDate;

public record UserAccountView(
        long userId,
        String email,
        String username,
        LocalDate birthday,
        String status,
        boolean accessible,
        Instant deletedAt,
        Instant createdAt,
        Instant updatedAt) {}
