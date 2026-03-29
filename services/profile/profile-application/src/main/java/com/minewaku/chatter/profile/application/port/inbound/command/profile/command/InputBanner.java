package com.minewaku.chatter.profile.application.port.inbound.command.profile.command;

import java.io.InputStream;
import java.util.Set;

import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage.StorableFile;
import com.minewaku.chatter.profile.domain.model.file.model.Namespace;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class InputBanner extends InputImage implements StorableFile {

    public static final Namespace NAMESPACE = Namespace.USER_BANNERS;

    private static final long MAX_SIZE_IN_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final double ASPECT_RATIO = 2.5; // rectangle
    private static final int MIN_WIDTH_IN_PIXELS = 320;
    private static final int MIN_HEIGHT_IN_PIXELS = 128; 
    private static final int MAX_WIDTH_IN_PIXELS = 2560;
    private static final int MAX_HEIGHT_IN_PIXELS = 1024;
    private static final Set<String> VALID_CONTENT_TYPES = Set.of(
        "png",
        "jpg",
        "jpeg",
        "gif",
        "webp"
    );

    public InputBanner(
        // @NonNull ObjectKey objectKey,
        // @NonNull UserId userId,
        @NonNull String originalFilename,
        @NonNull String contentType,
        long sizeInBytes,
        @NonNull InputStream contentStream
    ) {
        super(
            VALID_CONTENT_TYPES,
            originalFilename,
            contentType,
            sizeInBytes,
            contentStream
        );

        // this.objectKey = objectKey;
        validate();
    }

    private void validate() {
        if (this.sizeInBytes > InputBanner.MAX_SIZE_IN_BYTES) {
            throw new IllegalArgumentException("Banner file size exceeds the maximum limit of " + InputBanner.MAX_SIZE_IN_BYTES + " bytes.");
        }
        
        if (this.width < InputBanner.MIN_WIDTH_IN_PIXELS || this.height < InputBanner.MIN_HEIGHT_IN_PIXELS) {
            throw new IllegalArgumentException("Banner dimensions are too small. Minimum size is " + InputBanner.MIN_WIDTH_IN_PIXELS + "x" + InputBanner.MIN_HEIGHT_IN_PIXELS + " pixels.");
        }

        if (this.width > InputBanner.MAX_WIDTH_IN_PIXELS || this.height > InputBanner.MAX_HEIGHT_IN_PIXELS) {
            throw new IllegalArgumentException("Banner dimensions are too large. Maximum size is " + InputBanner.MAX_WIDTH_IN_PIXELS + "x" + InputBanner.MAX_HEIGHT_IN_PIXELS + " pixels.");
        }

        double actualAspectRatio = (double) this.width / (double) this.height;
        if (Math.abs(actualAspectRatio - InputBanner.ASPECT_RATIO) > 0.01) {
            throw new IllegalArgumentException("Banner must have an aspect ratio of " + InputBanner.ASPECT_RATIO + ":1 (square).");
        }
    }

    @Override
    public InputStream getInputStream() {
        return this.contentStream;
    }

    @Override
    public Namespace getNamespace() {
        return InputBanner.NAMESPACE;
    }
}
