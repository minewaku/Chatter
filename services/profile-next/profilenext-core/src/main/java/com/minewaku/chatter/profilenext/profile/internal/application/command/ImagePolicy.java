package com.minewaku.chatter.profilenext.profile.internal.application.command;

import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import java.time.Duration;
import java.util.Set;

/** Limits on uploaded files and on how long an upload permission or a notification stays acceptable. */
public record ImagePolicy(
        long maxBytesAvatar,
        long maxBytesBanner,
        Set<String> allowedFormats,
        Duration permissionLifetime,
        Duration webhookTolerance) {

    public long maxBytes(ImageSlot slot) {
        return slot == ImageSlot.AVATAR ? maxBytesAvatar : maxBytesBanner;
    }

    public boolean allows(String format) {
        return format != null && allowedFormats.contains(format.toLowerCase(java.util.Locale.ROOT));
    }
}
