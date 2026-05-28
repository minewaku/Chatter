package com.minewaku.chatter.profile.application.messaging.publisher.integration.event;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class AvatarFileStorageUploadedIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "None";
    public static final String EVENT_TYPE = "AvatarFileStorageUploaded";

    private final String fileHash;
    private final String namespace;
    private final Map<String, Object> context;
    private final Integer width;
    private final Integer height;
    private final Integer fileSize;

    public AvatarFileStorageUploadedIntegrationEvent(
                @NonNull String namespace, 
                Map<String, Object> context, 
                @NonNull String fileHash,
                Integer width,
                Integer height,
                Integer fileSize) {

        super(AGGREGATE_TYPE, EVENT_TYPE);

        this.namespace = namespace;
        this.context = context == null ? new HashMap<>() : context;
        this.fileHash = fileHash;
        this.width = width;
        this.height = height;
        this.fileSize = fileSize;
    }
    
}