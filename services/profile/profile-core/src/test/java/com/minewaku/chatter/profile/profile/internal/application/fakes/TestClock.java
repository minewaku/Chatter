package com.minewaku.chatter.profile.profile.internal.application.fakes;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock that only moves when a test says so. */
public class TestClock extends Clock {

    private Instant now;

    public TestClock(Instant start) {
        this.now = start;
    }

    public static TestClock at(String instant) {
        return new TestClock(Instant.parse(instant));
    }

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
