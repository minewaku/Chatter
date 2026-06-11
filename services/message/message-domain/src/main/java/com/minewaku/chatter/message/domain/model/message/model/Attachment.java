package com.minewaku.chatter.message.domain.model.message.model;

import org.springframework.data.annotation.PersistenceCreator;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class Attachment {
    
    private String fileHash;
    private String filename;
    private String contentType;
    private Long size;

    @PersistenceCreator
    public Attachment(
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

