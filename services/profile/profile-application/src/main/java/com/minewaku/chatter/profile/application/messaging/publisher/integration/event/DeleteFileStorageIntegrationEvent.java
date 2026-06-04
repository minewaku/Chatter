package com.minewaku.chatter.profile.application.messaging.publisher.integration.event;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class DeleteFileStorageIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "None";
    public static final String EVENT_TYPE = "DeleteFileStorage";

    private final String fileHash;
    private final String namespace;

    public DeleteFileStorageIntegrationEvent (
                @NonNull String fileHash,
                @NonNull String namespace) {

        super(AGGREGATE_TYPE, EVENT_TYPE);

        this.fileHash = fileHash;
        this.namespace = namespace;
    }
    
}
