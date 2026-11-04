package com.minewaku.chatter.profilenext.profile.internal.model;

/** A provider notification that passed its signature checks: who it is for, which slot, and what was stored. */
public record ConfirmedUpload(
        UserId userId, ImageSlot slot, String publicId, String url, String format, long bytes) {

    /** True when the public id is {@code chatter/profile/{slot folder}/{userId}/{uploadId}} for this user and slot. */
    public boolean publicIdMatchesOwner() {
        String[] parts = publicId.split("/");
        return parts.length == 5
                && parts[0].equals("chatter")
                && parts[1].equals("profile")
                && parts[2].equals(slot.folder())
                && parts[3].equals(Long.toString(userId.value()))
                && !parts[4].isBlank();
    }

    public StoredImage image() {
        return new StoredImage(publicId, url);
    }
}
