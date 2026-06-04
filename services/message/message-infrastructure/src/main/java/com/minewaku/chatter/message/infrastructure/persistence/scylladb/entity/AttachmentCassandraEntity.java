package com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity;

import org.springframework.data.annotation.PersistenceCreator;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class AttachmentCassandraEntity { 
    private String fileHash;
    private String filename;
    private String contentType;
    private Long size;
    private Integer position;

    @PersistenceCreator
    public AttachmentCassandraEntity(
            @NonNull String fileHash,
            @NonNull String filename,
            @NonNull String contentType,
            @NonNull Long size,
            @NonNull Integer position
    ) {
        this.fileHash = fileHash;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.position = position;
    }
}
