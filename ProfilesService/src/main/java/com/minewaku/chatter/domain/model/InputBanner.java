package com.minewaku.chatter.domain.model;

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
@EqualsAndHashCode
public class InputBanner {

    @NonNull
    private final StorageKey key;

    @NonNull
    private final InputImage inputImage;
    
    private static final StorageCategory STORAGE_CATEGORY = StorageCategory.USER_BANNER;
    private final long MAX_SIZE_IN_BYTES = 10 * 1024 * 1024; // 10 MB
    private final double ASPECT_RATIO = 2.5; // rectangle
    private final int MIN_WIDTH_IN_PIXELS = 320;
    private final int MIN_HEIGHT_IN_PIXELS = 128; 
    private final int MAX_WIDTH_IN_PIXELS = 2560;
    private final int MAX_HEIGHT_IN_PIXELS = 1024;
    private final Set<String> validContentTypes = Set.of(
        "png",
        "jpg",
        "jpeg",
        "gif",
        "webp"
    );

    public StorageCategory getStorageCategory() {
        return STORAGE_CATEGORY;
    } 

    public InputBanner(
        @NonNull StorageKey key,
        @NonNull InputImage inputImage
    ) {
        this.key = key;
        this.inputImage = inputImage;
        validate();
    }

    private void validate() {
        if (this.inputImage.getSizeInBytes() > this.MAX_SIZE_IN_BYTES) {
            throw new IllegalArgumentException("Banner file size exceeds the maximum limit of " + this.MAX_SIZE_IN_BYTES + " bytes.");
        }

        if (!this.validContentTypes.contains(this.inputImage.getContentType().toLowerCase())) {
            throw new IllegalArgumentException("Invalid banner content type: " + this.inputImage.getContentType());
        }

        if (this.inputImage.getWidth() < this.MIN_WIDTH_IN_PIXELS || this.inputImage.getHeight() < this.MIN_HEIGHT_IN_PIXELS) {
            throw new IllegalArgumentException("Banner dimensions are too small. Minimum size is " + this.MIN_WIDTH_IN_PIXELS + "x" + this.MIN_HEIGHT_IN_PIXELS + " pixels.");
        }

        if (this.inputImage.getWidth() > this.MAX_WIDTH_IN_PIXELS || this.inputImage.getHeight() > this.MAX_HEIGHT_IN_PIXELS) {
            throw new IllegalArgumentException("Banner dimensions are too large. Maximum size is " + this.MAX_WIDTH_IN_PIXELS + "x" + this.MAX_HEIGHT_IN_PIXELS + " pixels.");
        }

        double actualAspectRatio = (double) this.inputImage.getWidth() / (double) this.inputImage.getHeight();
        if (Math.abs(actualAspectRatio - this.ASPECT_RATIO) > 0.01) {
            throw new IllegalArgumentException("Banner must have an aspect ratio of " + this.ASPECT_RATIO + ":1 (rectangle).");
        }
    }
}
