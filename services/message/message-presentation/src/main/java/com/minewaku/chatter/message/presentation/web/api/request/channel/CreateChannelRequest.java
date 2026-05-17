package com.minewaku.chatter.message.presentation.web.api.request.channel;

public record CreateChannelRequest(
    String name,
    String description
) {
}
