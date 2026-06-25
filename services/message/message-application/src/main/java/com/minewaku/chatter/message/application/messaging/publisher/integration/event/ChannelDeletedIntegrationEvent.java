package com.minewaku.chatter.message.application.messaging.publisher.integration.event;

import lombok.Getter;

@Getter
public class ChannelDeletedIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "Channel";
    public static final String EVENT_TYPE = "ChannelDeleted";

    private final Long channelId;
    private final Long guildId;

    public ChannelDeletedIntegrationEvent (
            Long channelId,
            Long guildId) {

        super(AGGREGATE_TYPE, EVENT_TYPE);

        this.channelId = channelId;
        this.guildId = guildId;
    }
    
}