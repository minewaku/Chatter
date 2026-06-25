package com.minewaku.chatter.message.application.messaging.publisher.integration.event;

import java.time.Instant;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class MessageCreatedIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "Message";
    public static final String EVENT_TYPE = "MessageCreated";

    private Long id;
    private Long guildId;
    private Long channelId;
    private Long userId;
    private Long replyId;
    private String content;
    private Instant timestamp;


    public MessageCreatedIntegrationEvent(
            @NonNull Long id,
            @NonNull Long guildId,
            @NonNull Long channelId,
            @NonNull Long userId,
            Long replyId,
            @NonNull String content,
            @NonNull Instant timestamp) {

        super(AGGREGATE_TYPE, EVENT_TYPE);

        this.id = id;
        this.guildId = guildId;
        this.channelId = channelId;
        this.userId = userId;
        this.replyId = replyId;
        this.content = content;
        this.timestamp = timestamp;
    }
}
