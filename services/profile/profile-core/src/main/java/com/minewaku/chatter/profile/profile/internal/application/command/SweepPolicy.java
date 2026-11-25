package com.minewaku.chatter.profile.profile.internal.application.command;

import java.time.Duration;

/** How one sweep run behaves: how many keys it handles at most, and how long a failed delete waits before its next try. */
public record SweepPolicy(int batchSize, Duration retryDelay) {}
