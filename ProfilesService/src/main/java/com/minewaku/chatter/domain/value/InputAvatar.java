package com.minewaku.chatter.domain.value;

import java.io.InputStream;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class InputAvatar extends InputImage {

    private final long maxSizeInBytes = 10 * 1024 * 1024; // 10 MB
    private final double aspectRatio = 1.0; // square
    private final int minWidthInPixels = 128;
    private final int minHeightInPixels = 128; 
    private final int maxWidthInPixels = 1024;
    private final int maxHeightInPixels = 1024;
    private final Set<String> validContentTypes = Set.of(
        "png",
        "jpg",
        "jpeg",
        "gif",
        "webp",
        "avif"
    );

    public InputAvatar(
        @NonNull String originalFilename,
        @NonNull String contentType,
        long sizeInBytes,
        @NonNull InputStream contentStream
    ) {
        super(
            originalFilename,
            contentType,
            sizeInBytes,
            contentStream
        );

        validate();
    }

    private void validate() {
        if (this.sizeInBytes > this.maxSizeInBytes) {
            throw new IllegalArgumentException("Avatar file size exceeds the maximum limit of " + this.maxSizeInBytes + " bytes.");
        }

        if (!this.validContentTypes.contains(this.contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid avatar content type: " + this.contentType);
        }

        if (this.width < this.minWidthInPixels || this.height < this.minHeightInPixels) {
            throw new IllegalArgumentException("Avatar dimensions are too small. Minimum size is " + this.minWidthInPixels + "x" + this.minHeightInPixels + " pixels.");
        }

        if (this.width > this.maxWidthInPixels || this.height > this.maxHeightInPixels) {
            throw new IllegalArgumentException("Avatar dimensions are too large. Maximum size is " + this.maxWidthInPixels + "x" + this.maxHeightInPixels + " pixels.");
        }

        double actualAspectRatio = (double) this.width / (double) this.height;
        if (Math.abs(actualAspectRatio - this.aspectRatio) > 0.01) {
            throw new IllegalArgumentException("Avatar must have an aspect ratio of " + this.aspectRatio + ":1 (square).");
        }
    }
}
