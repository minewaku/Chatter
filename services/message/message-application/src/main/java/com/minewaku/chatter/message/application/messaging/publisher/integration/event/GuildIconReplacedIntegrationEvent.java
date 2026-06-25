package com.minewaku.chatter.message.application.messaging.publisher.integration.event;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class GuildIconReplacedIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "Guild";
    public static final String EVENT_TYPE = "GuildIconReplaced";

    private final String guildId;
    private final String oldIconHash;
    private final String namespace;
    private final String fileHash;
    private final String contentType;
    private final String fileName;
    private final Integer fileSize;

    public GuildIconReplacedIntegrationEvent(
            @NonNull String guildId,
            String oldIconHash,
            @NonNull String namespace,
            @NonNull String fileHash,
            @NonNull String contentType,
            @NonNull String fileName,
            @NonNull Integer fileSize
    ) {
        super(AGGREGATE_TYPE, EVENT_TYPE);

        this.guildId = guildId;
        this.oldIconHash = oldIconHash;
        this.namespace = namespace;
        this.fileHash = fileHash;
        this.contentType = contentType;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }
}
