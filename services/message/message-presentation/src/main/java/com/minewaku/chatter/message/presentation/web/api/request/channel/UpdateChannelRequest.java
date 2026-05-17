package com.minewaku.chatter.message.presentation.web.api.request.channel;

public record UpdateChannelRequest (
    String name,
    String description
) {
    
}
