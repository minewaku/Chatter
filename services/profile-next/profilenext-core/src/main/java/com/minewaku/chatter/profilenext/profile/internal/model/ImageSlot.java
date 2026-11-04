package com.minewaku.chatter.profilenext.profile.internal.model;

/** The two places a profile holds an image. {@code folder} is the segment used in the storage path. */
public enum ImageSlot {
    AVATAR("avatars"),
    BANNER("banners");

    private final String folder;

    ImageSlot(String folder) {
        this.folder = folder;
    }

    public String folder() {
        return folder;
    }
}
