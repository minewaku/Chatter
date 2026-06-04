package com.minewaku.chatter.profile.application.messaging.publisher.integration.event;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class PersistFileStorageIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "Asset";
    public static final String EVENT_TYPE = "PersistFileStorage";

    private final Long id;

    private final String namespace;
    private final String fileHash;

    public PersistFileStorageIntegrationEvent(
            @NonNull Long id, 
            @NonNull String namespace,
            @NonNull String fileHash) {
                
        super(AGGREGATE_TYPE, EVENT_TYPE);
        this.id = id;
        this.namespace = namespace;
        this.fileHash = fileHash;
    }
}

