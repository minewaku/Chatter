package com.minewaku.chatter.profile.application.messaging.publisher.integration.event;

import lombok.NonNull;

public class AssetUploadedIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "Asset";
    public static final String EVENT_TYPE = "AssetUploaded";

    @NonNull
    private final Long id;

    @NonNull
    private final String namespace;

    @NonNull
    private final String fileHash;

    public AssetUploadedIntegrationEvent(Long id, String namespace, String fileHash) {
        super(AGGREGATE_TYPE, EVENT_TYPE);
        this.id = id;
        this.namespace = namespace;
        this.fileHash = fileHash;
    }
}
