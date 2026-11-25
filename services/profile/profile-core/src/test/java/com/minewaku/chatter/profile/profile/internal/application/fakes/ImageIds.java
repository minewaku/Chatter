package com.minewaku.chatter.profile.profile.internal.application.fakes;

import com.minewaku.chatter.profile.profile.internal.model.ImageId;
import java.util.UUID;

/** Deterministic, strictly increasing UUIDv7-shaped ids for tests: {@code at(2)} is newer than {@code at(1)}. */
public final class ImageIds {

    private ImageIds() {}

    public static ImageId at(int n) {
        return new ImageId(new UUID(((long) n << 16) | 0x7000L, 0x8000_0000_0000_0000L | n));
    }
}
