package com.minewaku.chatter.message.application.messaging.publisher.integration.event;

import java.util.Map;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class AssetCreatedIntegrationEvent extends IntegrationEvent {
    public static final String AGGREGATE_TYPE = "Asset";
    public static final String EVENT_TYPE = "AssetCreated";

    private final Long id;
    private final String namespace;
    private final String fileHash;
    private final Map<String, Object> context;

    public AssetCreatedIntegrationEvent(
            @NonNull Long id, 
            @NonNull String namespace, 
            @NonNull String fileHash,
            @NonNull Map<String, Object> context) {
                
        super(AGGREGATE_TYPE, EVENT_TYPE);
        this.id = id;
        this.namespace = namespace;
        this.fileHash = fileHash;
        this.context = context;
    }
}
