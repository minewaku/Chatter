package com.minewaku.chatter.profile.application.port.inbound.command.profile.command;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.minewaku.chatter.profile.domain.sharedkernel.exception.BusinessRuleViolationException;

import lombok.Getter;
import lombok.NonNull;

@Getter
public abstract class InputImage extends InputFile {

    protected int width;
    protected int height;
    protected String formatName; 
    private final Set<String> validContentTypes;
    
    public InputImage (
        @NonNull Set<String> validContentTypes,
        @NonNull String originalFilename,
        @NonNull String contentType,
        long sizeInBytes,
        @NonNull InputStreamSupplier streamSupplier // Chuyển sang Supplier
    ) {
        super(originalFilename, contentType, sizeInBytes, streamSupplier);
        this.validContentTypes = validContentTypes;
        this.extractImageMetadata();
    }

    private void extractImageMetadata() {
        try (InputStream metadataStream = this.openStream();
             ImageInputStream iis = ImageIO.createImageInputStream(metadataStream)) {
             
            if (iis == null) {
                throw new BusinessRuleViolationException("Could not create an image input stream from the provided file.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(iis, true, true);
                    this.width = reader.getWidth(0);
                    this.height = reader.getHeight(0);
                    this.formatName = reader.getFormatName().toLowerCase();

                    if (!isFormatSupported(this.formatName)) {
                        throw new BusinessRuleViolationException("Unsupported image format: " + this.formatName);
                    }
                } finally {
                    reader.dispose();
                }
            } else {
                throw new BusinessRuleViolationException("Unable to recognize image format.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image info for metadata extraction", e);
        }
    }
    
    private boolean isFormatSupported(String detectedFormat) {
        if (validContentTypes.contains(detectedFormat)) return true;
        if ("jpeg".equals(detectedFormat) && validContentTypes.contains("jpg")) return true;
        if ("jpg".equals(detectedFormat) && validContentTypes.contains("jpeg")) return true;
        
        return false;
    }
}