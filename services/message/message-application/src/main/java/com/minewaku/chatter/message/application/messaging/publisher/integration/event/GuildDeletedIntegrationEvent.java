package com.minewaku.chatter.message.application.messaging.publisher.integration.event;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class GuildDeletedIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "Guild";
    public static final String EVENT_TYPE = "GuildDeleted";

    private final Long guildId;

    public GuildDeletedIntegrationEvent (
                @NonNull Long guildId) {

        super(AGGREGATE_TYPE, EVENT_TYPE);

        this.guildId = guildId;
    }
    
}
