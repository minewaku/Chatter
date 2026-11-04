package com.minewaku.chatter.profilenext.profile.internal.application.command;

import java.time.Duration;

/** A stored file younger than {@code gracePeriod} is never swept, so a pending upload is not removed early. */
public record SweepPolicy(Duration gracePeriod) {}
