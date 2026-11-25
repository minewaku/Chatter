package com.minewaku.chatter.profile.profile.internal.model;

import java.util.Locale;

/**
 * Where an image lives, as a provider-neutral relative path: {@code profiles/{userId}/{slot}/{imageId}}. Core owns the
 * layout so the storage adapter (which turns it into an object key) and the public address builder agree on it; neither
 * adds anything but a bucket or a base address. The path is built from typed values only, so no request text reaches it.
 */
public final class ImagePath {

    private ImagePath() {}

    public static String of(OwnedImage image) {
        return of(image.userId(), image.slot(), image.imageId());
    }

    public static String of(UserId userId, ImageSlot slot, ImageId imageId) {
        return "profiles/" + userId.value() + "/" + slot.name().toLowerCase(Locale.ROOT) + "/" + imageId.value();
    }
}
