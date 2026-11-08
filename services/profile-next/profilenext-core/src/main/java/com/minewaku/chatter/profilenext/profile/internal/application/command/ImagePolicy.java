package com.minewaku.chatter.profilenext.profile.internal.application.command;

import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.ConfirmedUpload;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * Limits on uploaded files and on how long an upload permission or a notification stays acceptable. The dimension
 * limits repeat what the provider already enforces at upload; checking them again is a second line of defence.
 */
public record ImagePolicy(
        long maxBytesAvatar,
        long maxBytesBanner,
        int maxDimensionAvatar,
        int maxDimensionBanner,
        Set<String> allowedFormats,
        Duration permissionLifetime,
        Duration webhookTolerance) {

    public long maxBytes(ImageSlot slot) {
        return slot == ImageSlot.AVATAR ? maxBytesAvatar : maxBytesBanner;
    }

    public int maxDimension(ImageSlot slot) {
        return slot == ImageSlot.AVATAR ? maxDimensionAvatar : maxDimensionBanner;
    }

    public boolean allows(String format) {
        return format != null && allowedFormats.contains(format.toLowerCase(Locale.ROOT));
    }

    /** True when the format is allowed and bytes, width and height are all within the limits of the upload's slot. */
    public boolean permits(ConfirmedUpload upload) {
        int limit = maxDimension(upload.slot());
        return allows(upload.format())
                && upload.bytes() <= maxBytes(upload.slot())
                && upload.dimensions().width() <= limit
                && upload.dimensions().height() <= limit;
    }
}
