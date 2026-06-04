package com.minewaku.chatter.profile.application.messaging.publisher.integration.event;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class BannerFileStorageUploadedIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "None";
    public static final String EVENT_TYPE = "BannerFileStorageUploaded";

    private final String fileHash;
    private final String namespace;
    private final Map<String, Object> context;
    private final String contentType;
    private final String fileName;
    private final Integer fileSize;

    public BannerFileStorageUploadedIntegrationEvent(
                @NonNull String namespace, 
                Map<String, Object> context, 
                @NonNull String fileHash,
                @NonNull String contentType,
                @NonNull String fileName,
                Integer fileSize) {

        super(AGGREGATE_TYPE, EVENT_TYPE);

        this.namespace = namespace;
        this.context = context == null ? new HashMap<>() : context;
        this.fileHash = fileHash;
        this.contentType = contentType;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }
    
}
