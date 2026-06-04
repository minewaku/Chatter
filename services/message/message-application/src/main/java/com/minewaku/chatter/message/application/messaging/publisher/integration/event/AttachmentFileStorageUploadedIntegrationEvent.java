package com.minewaku.chatter.message.application.messaging.publisher.integration.event;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class AttachmentFileStorageUploadedIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "None";
    public static final String EVENT_TYPE = "AttachmentFileStorageUploaded";

    private final String fileHash;
    private final String namespace;
    private final Map<String, Object> context;
    private final String contentType;
    private final Integer fileSize;

    public AttachmentFileStorageUploadedIntegrationEvent(
                @NonNull String namespace, 
                Map<String, Object> context, 
                @NonNull String fileHash,
                @NonNull String contentType,
                Integer fileSize) {

        super(AGGREGATE_TYPE, EVENT_TYPE);

        this.namespace = namespace;
        this.context = context == null ? new HashMap<>() : context;
        this.fileHash = fileHash;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }
    
}