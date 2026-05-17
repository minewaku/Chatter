package com.minewaku.chatter.message.application.messaging.publisher.integration.event;

import lombok.NonNull;

public class AssetDeletedIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "Asset";
    public static final String EVENT_TYPE = "AssetDeleted";

    @NonNull
    private final Long id;

    @NonNull
    private final String namespace;

    @NonNull
    private final String fileHash;

    public AssetDeletedIntegrationEvent(Long id, String namespace, String fileHash) {
        super(AGGREGATE_TYPE, EVENT_TYPE);
        this.id = id;
        this.namespace = namespace;
        this.fileHash = fileHash;
    }
}
