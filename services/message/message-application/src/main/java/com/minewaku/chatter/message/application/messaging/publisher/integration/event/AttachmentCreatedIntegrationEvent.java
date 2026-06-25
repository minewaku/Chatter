package com.minewaku.chatter.message.application.messaging.publisher.integration.event;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class AttachmentCreatedIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "None";
    public static final String EVENT_TYPE = "AttachmentCreated";

    private final String messageId;
    private final String channelId;
    private final String namespace;
    private final String fileHash;
    private final String contentType;
    private final String fileName;
    private final Integer fileSize;



    public AttachmentCreatedIntegrationEvent(
            @NonNull String messageId,
            @NonNull String channelId,
            @NonNull String namespace,
            @NonNull String fileHash,
            @NonNull String contentType,
            @NonNull String fileName,
            @NonNull Integer fileSize
    ) {
        super(AGGREGATE_TYPE, EVENT_TYPE);

        this.messageId = messageId;
        this.channelId = channelId;
        this.namespace = namespace;
        this.fileHash = fileHash;
        this.contentType = contentType;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

}