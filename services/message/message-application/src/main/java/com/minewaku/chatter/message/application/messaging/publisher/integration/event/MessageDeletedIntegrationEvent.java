package com.minewaku.chatter.message.application.messaging.publisher.integration.event;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class MessageDeletedIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "Message";
    public static final String EVENT_TYPE = "MessageDeleted";

    private Long id;
    private Long channelId;
    private Long userId;


    public MessageDeletedIntegrationEvent(
            @NonNull Long id,
            @NonNull Long channelId,
            @NonNull Long userId) {

        super(AGGREGATE_TYPE, EVENT_TYPE);

        this.id = id;
        this.channelId = channelId;
        this.userId = userId;
    }
}
