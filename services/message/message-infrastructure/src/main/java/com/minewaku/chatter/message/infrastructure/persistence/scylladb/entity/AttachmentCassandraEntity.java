package com.minewaku.chatter.message.infrastructure.persistence.scylladb.entity;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

import lombok.Getter;
import lombok.NonNull;

@Getter
@UserDefinedType("attachment")
public class AttachmentCassandraEntity {

    @Column("file_hash")
    private String fileHash;

    @Column("filename")
    private String filename;

    @Column("content_type")
    private String contentType;

    @Column("size")
    private Long size;

    @PersistenceCreator
    public AttachmentCassandraEntity(
            @NonNull String fileHash,
            @NonNull String filename,
            @NonNull String contentType,
            @NonNull Long size
    ) {
        this.fileHash = fileHash;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
    }
}
