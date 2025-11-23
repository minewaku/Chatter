package com.minewaku.chatter.adapter.web.response;

import java.time.Instant;

public record RoleDTO(
    long id,
    String name,
    String code,
    String description,
    Instant deletedAt,
    Instant createdAt,
	Instant modifiedAt) {
}
