package com.minewaku.chatter.profile.application.port.inbound.command.profile.command;

import java.io.IOException;
import java.io.InputStream;

import com.minewaku.chatter.profile.domain.sharedkernel.exception.BusinessRuleViolationException;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
public abstract class InputFile { 
    
    protected final String originalFilename;
    protected final String contentType; // which is included in the header of the request/response.
    protected final long sizeInBytes;

    @ToString.Exclude
    private final InputStreamSupplier streamSupplier;

    protected InputFile(
        @NonNull String originalFilename,
        @NonNull String contentType,
        long sizeInBytes,
        @NonNull InputStreamSupplier streamSupplier
    ) {
        if (sizeInBytes <= 0) {
            throw new BusinessRuleViolationException("File cannot be empty");
        }
        
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeInBytes = sizeInBytes;
        this.streamSupplier = streamSupplier;
    }

    public InputStream openStream() throws IOException {
        return streamSupplier.get();
    }

    @FunctionalInterface
    public interface InputStreamSupplier {
        InputStream get() throws IOException;
    }
}
