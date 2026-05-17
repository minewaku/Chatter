package com.minewaku.chatter.message.presentation.web.api.request.guild;

public record UpdateGuildRequest(
    String name,
    String description
) {
}
