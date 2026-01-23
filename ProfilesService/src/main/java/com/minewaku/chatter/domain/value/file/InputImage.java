package com.minewaku.chatter.domain.value.file;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.minewaku.chatter.domain.exception.BusinessRuleViolationException;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class InputImage extends InputFile {

    protected int width;
    protected int height;
    protected String formatName;

    private static final Set<String> SUPPORTED_FORMATS = Set.of(
        "png",
        "jpeg",
        "jpg",
        "gif",
        "webp"
    );

    public InputImage (
        @NonNull String originalFilename,
        @NonNull String contentType,
        long sizeInBytes,
        @NonNull InputStream contentStream
    ) {
        super(originalFilename, contentType, sizeInBytes, ensureMarkSupported(contentStream));
        
        this.extractImageMetadata();
    }


    // Explain about Stream and BufferedInputStream:
    // 1. Memory Efficiency: Files are processed as Streams instead of byte[] to save memory. 
    //    Loading an entire file (byte[]) into memory can cause OutOfMemory errors. 
    //    Streams allow processing data chunk-by-chunk.
    //
    // 2. Why Buffered?: Standard Streams are "forward-only" (read-once). 
    //    If you read data to check the header, that data is consumed and lost.
    //    BufferedInputStream wraps the original stream and caches read data, 
    //    enabling the mark/reset function to "rewind" to the beginning after validation.
    private static InputStream ensureMarkSupported(InputStream stream) {
        return stream.markSupported() ? stream : new BufferedInputStream(stream);
    }


    // Doesn't need to load the entire image into memory, just reads metadata.
    private void extractImageMetadata() {
        try {
            this.contentStream.mark((int) this.sizeInBytes + 1);
            ImageIO.setUseCache(false); 

            try (ImageInputStream iis = ImageIO.createImageInputStream(this.contentStream)) {
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
            }

            this.contentStream.reset();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image info", e);
        }
    }

    
    private boolean isFormatSupported(String detectedFormat) {
        if (SUPPORTED_FORMATS.contains(detectedFormat)) {
            return true;
        }
        if ("jpeg".equals(detectedFormat) && SUPPORTED_FORMATS.contains("jpg")) return true;
        if ("jpg".equals(detectedFormat) && SUPPORTED_FORMATS.contains("jpeg")) return true;
        
        return false;
    }
}