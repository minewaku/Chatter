package com.minewaku.chatter.message.application.messaging.publisher.integration.event;

import lombok.Getter;

@Getter
public class ChannelCreatedIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "Channel";
    public static final String EVENT_TYPE = "ChannelCreated";

    private final Long channelId;
    private final Long guildId;
    private final String name;
    private final String description;

    public ChannelCreatedIntegrationEvent (
            Long channelId,
            Long guildId,
            String name,
            String description) {

        super(AGGREGATE_TYPE, EVENT_TYPE);

        this.channelId = channelId;
        this.guildId = guildId;
        this.name = name;
        this.description = description;
    }
    
}
