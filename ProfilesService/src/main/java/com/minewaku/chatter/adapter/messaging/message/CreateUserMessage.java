package com.minewaku.chatter.adapter.messaging.message;

public record CreateUserMessage(
        long id,
        String email,
        String username,
        String birthday,
        boolean enabled,
        boolean locked,
        boolean deleted,
        String deletedAt,
        String createdAt,
        String modifiedAt
) {
    
}
