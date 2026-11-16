package com.minewaku.chatter.profilenext.profile.internal.application.fakes;

import com.minewaku.chatter.profilenext.profile.internal.application.command.ImagePolicy;
import java.time.Duration;
import java.util.Set;

public final class Policies {

    private Policies() {}

    /** 5 MB avatar / 10 MB banner, up to 1024 / 2048 pixels, 5 minute tickets, 10 minute grace. */
    public static ImagePolicy standard() {
        return new ImagePolicy(
                5_000_000,
                10_000_000,
                1024,
                2048,
                Set.of("jpg", "png", "webp"),
                Duration.ofMinutes(5),
                Duration.ofMinutes(10),
                65_536);
    }
}
