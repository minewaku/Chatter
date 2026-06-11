package com.minewaku.chatter.message.application.messaging.publisher.integration.event;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class AssetDetachedIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "Asset";
    public static final String EVENT_TYPE = "AssetDetached";

    private final String fileHash;
    private final String namespace;

    public AssetDetachedIntegrationEvent(
                @NonNull String namespace, 
                @NonNull String fileHash) {

        super(AGGREGATE_TYPE, EVENT_TYPE);

        this.namespace = namespace;
        this.fileHash = fileHash;
    }
}