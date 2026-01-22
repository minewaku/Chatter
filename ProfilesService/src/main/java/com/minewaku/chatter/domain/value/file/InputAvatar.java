package com.minewaku.chatter.domain.value.file;

import java.io.InputStream;
import java.util.Set;

import com.minewaku.chatter.domain.value.id.StorageKey;
import com.minewaku.chatter.domain.value.id.UserId;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class InputAvatar extends InputImage implements StorableFile {

    private final StorageKey key;
    private final UserId userId;

    private static final long MAX_SIZE_IN_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final double ASPECT_RATIO = 1.0;
    private static final int MIN_WIDTH_IN_PIXELS = 128;
    private static final int MIN_HEIGHT_IN_PIXELS = 128; 
    private static final int MAX_WIDTH_IN_PIXELS = 1024;
    private static final int MAX_HEIGHT_IN_PIXELS = 1024;
    private static final Set<String> VALID_CONTENT_TYPES = Set.of(
        "png",
        "jpg",
        "jpeg",
        "gif",
        "webp"
    );

    public InputAvatar(
        @NonNull StorageKey key,
        @NonNull UserId userId,
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
        this.userId = userId;
        validate();
    }

    private void validate() {
        if (this.sizeInBytes > InputAvatar.MAX_SIZE_IN_BYTES) {
            throw new IllegalArgumentException("Avatar file size exceeds the maximum limit of " + InputAvatar.MAX_SIZE_IN_BYTES + " bytes.");
        }

        if (!InputAvatar.VALID_CONTENT_TYPES.contains(this.contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid avatar content type: " + this.contentType.toLowerCase());
        }

        if (this.width < InputAvatar.MIN_WIDTH_IN_PIXELS || this.height < InputAvatar.MIN_HEIGHT_IN_PIXELS) {
            throw new IllegalArgumentException("Avatar dimensions are too small. Minimum size is " + InputAvatar.MIN_WIDTH_IN_PIXELS + "x" + InputAvatar.MIN_HEIGHT_IN_PIXELS + " pixels.");
        }

        if (this.width > InputAvatar.MAX_WIDTH_IN_PIXELS || this.height > InputAvatar.MAX_HEIGHT_IN_PIXELS) {
            throw new IllegalArgumentException("Avatar dimensions are too large. Maximum size is " + InputAvatar.MAX_WIDTH_IN_PIXELS + "x" + InputAvatar.MAX_HEIGHT_IN_PIXELS + " pixels.");
        }

        double actualAspectRatio = (double) this.width / (double) this.height;
        if (Math.abs(actualAspectRatio - InputAvatar.ASPECT_RATIO) > 0.01) {
            throw new IllegalArgumentException("Avatar must have an aspect ratio of " + InputAvatar.ASPECT_RATIO + ":1 (square).");
        }
    }

    @Override
    public InputStream getInputStream() {
        return this.contentStream;
    }

    @Override
    public StorageKey getStorageKey() {
        return this.key;
    }

    @Override
    public String getPath() {
        return "users/" + this.userId.getValue().toString() + "/avatar/" + this.key.getValue().toString();  
    }
}
