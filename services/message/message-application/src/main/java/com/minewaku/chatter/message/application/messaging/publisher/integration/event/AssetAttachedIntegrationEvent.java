package com.minewaku.chatter.message.application.messaging.publisher.integration.event;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class AssetAttachedIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "Asset";
    public static final String EVENT_TYPE = "AssetAttached";
    
    private final String namespace;
    private final String fileHash;
    private final String contentType;
    private final String fileName;
    private final Integer fileSize;

    public AssetAttachedIntegrationEvent(
            @NonNull String namespace,
            @NonNull String fileHash,
            @NonNull String contentType,
            @NonNull String fileName,
            @NonNull Integer fileSize) {

        super(AGGREGATE_TYPE, EVENT_TYPE);

        this.namespace = namespace;
        this.fileHash = fileHash;
        this.contentType = contentType;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }
}
