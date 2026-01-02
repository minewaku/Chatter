package com.minewaku.chatter.domain.value;
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

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "image/png",
        "image/jpeg",
        "image/jpg",
        "image/gif",
        "image/webp",
        "image/avif"
    );

    private static final Set<String> SUPPORTED_FORMATS = Set.of(
        "png",
        "jpeg",
        "jpg",
        "gif",
        "webp", // Cần thư viện hỗ trợ (vd: TwelveMonkeys) mới đọc được
        "avif"  // Cần thư viện hỗ trợ mới đọc được
    );

    public InputImage(
        @NonNull String originalFilename,
        @NonNull String contentType,
        long sizeInBytes,
        @NonNull InputStream contentStream
    ) {
        super(originalFilename, contentType, sizeInBytes, contentStream);
        this.extractImageMetadata();
        this.validateMimeType(contentType);
    }

    private void extractImageMetadata() {
        try {
            if (!this.contentStream.markSupported()) {
                throw new IllegalStateException("Stream does not support mark/reset");
            }

            this.contentStream.mark((int) this.sizeInBytes + 1);
            try (ImageInputStream iis = ImageIO.createImageInputStream(this.contentStream)) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(iis, true);
                        this.width = reader.getWidth(0);
                        this.height = reader.getHeight(0);
                        String detectedFormat = reader.getFormatName().toLowerCase();

                        if (!isFormatSupported(detectedFormat)) {
                             throw new BusinessRuleViolationException("Unsupported image format: " + detectedFormat);
                        }
                        
                        this.formatName = detectedFormat;

                    } finally {
                        reader.dispose();
                    }
                } else {
                    throw new BusinessRuleViolationException("Unable to recognize image format. Please ensure the file is a valid image.");
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

    private void validateMimeType(String contentType) {
        String normalizedType = contentType.toLowerCase().trim();
        
        if (!ALLOWED_MIME_TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException("Invalid content type: " + contentType + ". Only image files are allowed.");
        }
    }
}