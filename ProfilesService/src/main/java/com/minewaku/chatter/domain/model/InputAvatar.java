package com.minewaku.chatter.domain.model;

import java.io.InputStream;
import java.util.Set;

import com.minewaku.chatter.domain.value.InputImage;
import com.minewaku.chatter.domain.value.StorageCategory;
import com.minewaku.chatter.domain.value.id.StorageKey;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class InputAvatar extends InputImage {

    @NonNull
    private final StorageKey key;

    private static final StorageCategory STORAGE_CATEGORY = StorageCategory.USER_AVATAR;
    private final long MAX_SIZE_IN_BYTES = 10 * 1024 * 1024; // 10 MB
    private final double ASPECT_RATIO = 1.0;
    private final int MIN_WIDTH_IN_PIXELS = 128;
    private final int MIN_HEIGHT_IN_PIXELS = 128; 
    private final int MAX_WIDTH_IN_PIXELS = 1024;
    private final int MAX_HEIGHT_IN_PIXELS = 1024;
    private final Set<String> SUPPORTED_FORMATS = Set.of(
        "png",
        "jpg",
        "jpeg",
        "gif",
        "webp"
    );

    public StorageCategory getStorageCategory() {
        return STORAGE_CATEGORY;
    } 

    public InputAvatar(
        @NonNull StorageKey key,
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

        this.key = key;
        validate();
    }

    private void validate() {
        if (this.sizeInBytes > this.MAX_SIZE_IN_BYTES) {
            throw new IllegalArgumentException("Avatar file size exceeds the maximum limit of " + this.MAX_SIZE_IN_BYTES + " bytes.");
        }

        if (!this.SUPPORTED_FORMATS.contains(this.contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid avatar content type: " + this.contentType);
        }

        if (this.width < this.MIN_WIDTH_IN_PIXELS || this.height < this.MIN_HEIGHT_IN_PIXELS) {
            throw new IllegalArgumentException("Avatar dimensions are too small. Minimum size is " + this.MIN_WIDTH_IN_PIXELS + "x" + this.MIN_HEIGHT_IN_PIXELS + " pixels.");
        }

        if (this.width > this.MAX_WIDTH_IN_PIXELS || this.height > this.MAX_HEIGHT_IN_PIXELS) {
            throw new IllegalArgumentException("Avatar dimensions are too large. Maximum size is " + this.MAX_WIDTH_IN_PIXELS + "x" + this.MAX_HEIGHT_IN_PIXELS + " pixels.");
        }

        double actualAspectRatio = (double) this.width / (double) this.height;
        if (Math.abs(actualAspectRatio - this.ASPECT_RATIO) > 0.01) {
            throw new IllegalArgumentException("Avatar must have an aspect ratio of " + this.ASPECT_RATIO + ":1 (square).");
        }
    }
}
