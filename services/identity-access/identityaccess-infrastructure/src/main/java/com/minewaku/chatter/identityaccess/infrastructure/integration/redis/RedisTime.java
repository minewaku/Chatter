package com.minewaku.chatter.identityaccess.infrastructure.integration.redis;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Instants are stored as epoch microseconds: exact in Lua doubles and equal to PostgreSQL precision. */
public final class RedisTime {

    private RedisTime() {}

    public static long toMicros(Instant instant) {
        return ChronoUnit.MICROS.between(Instant.EPOCH, instant);
    }

    public static Instant fromMicros(long micros) {
        return Instant.EPOCH.plus(micros, ChronoUnit.MICROS);
    }

    public static long toMillisFloor(long micros) {
        return Math.floorDiv(micros, 1_000L);
    }
}
