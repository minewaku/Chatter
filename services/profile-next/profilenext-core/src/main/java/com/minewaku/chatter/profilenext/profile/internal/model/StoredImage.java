package com.minewaku.chatter.profilenext.profile.internal.model;

/** An image kept at the storage provider: where it is and the address it is served from. */
public record StoredImage(String publicId, String url) {

    /** The last path segment of the public id: a time-ordered id, so ids of one format compare in time order. */
    public String uploadId() {
        return publicId.substring(publicId.lastIndexOf('/') + 1);
    }
}
