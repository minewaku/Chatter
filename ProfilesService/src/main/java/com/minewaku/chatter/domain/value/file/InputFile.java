package com.minewaku.chatter.domain.value.file;

import java.io.InputStream;

import com.minewaku.chatter.domain.exception.BusinessRuleViolationException;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
public abstract class InputFile { 
    
    protected final String originalFilename;
    protected final String contentType;
    protected final long sizeInBytes;
    protected final InputStream contentStream;

    protected InputFile(
        @NonNull String originalFilename,
        @NonNull String contentType,
        long sizeInBytes,
        @NonNull InputStream contentStream
    ) {
        if (sizeInBytes <= 0) {
            throw new BusinessRuleViolationException("File cannot be empty");
        }
        
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeInBytes = sizeInBytes;
        this.contentStream = contentStream;
    }
}
