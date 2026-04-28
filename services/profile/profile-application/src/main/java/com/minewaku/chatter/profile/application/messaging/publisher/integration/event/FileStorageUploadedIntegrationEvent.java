package com.minewaku.chatter.profile.application.messaging.publisher.integration.event;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class FileStorageUploadedIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "None";
    public static final String EVENT_TYPE = "FileStorageUploaded";

    @NonNull
    private final String namespace;

    @NonNull
    private final Map<String, Object> context;

    @NonNull
    private final String fileHash;

    private final Integer width;
    private final Integer height;
    private final Integer size;

    public FileStorageUploadedIntegrationEvent(
                @NonNull String namespace, 
                Map<String, Object> context, 
                @NonNull String fileHash,
                Integer width,
                Integer height,
                Integer size) {

        super(AGGREGATE_TYPE, EVENT_TYPE);

        this.namespace = namespace;
        this.context = context == null ? new HashMap<>() : context;
        this.fileHash = fileHash;
        this.width = width;
        this.height = height;
        this.size = size;
    }
}
