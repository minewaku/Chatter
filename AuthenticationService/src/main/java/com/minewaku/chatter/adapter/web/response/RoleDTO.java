package com.minewaku.chatter.adapter.web.response;

import java.time.Instant;

public record RoleDto(
        long id,
        String name,
        String code,
        String description,
        Instant createdAt,
        Instant modifiedAt,
        Boolean isDeleted,
        Instant deletedAt) {
}
