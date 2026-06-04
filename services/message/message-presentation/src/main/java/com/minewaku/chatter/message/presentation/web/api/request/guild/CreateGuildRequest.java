package com.minewaku.chatter.message.presentation.web.api.request.guild;

public record CreateGuildRequest(
    String name,
    String description
) {
}

