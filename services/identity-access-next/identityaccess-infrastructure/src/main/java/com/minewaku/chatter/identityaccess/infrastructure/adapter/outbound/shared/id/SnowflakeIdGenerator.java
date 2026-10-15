package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.shared.id;

import com.minewaku.chatter.identityaccess.shared.port.TimeBasedIdGenerator;
import java.time.Clock;
import org.springframework.stereotype.Component;

/**
 * 41-bit millisecond timestamp (since a custom epoch), 10-bit worker id from configuration, 12-bit sequence.
 * Ids are positive and strictly increasing per instance. A small clock rollback is waited out; a larger one fails.
 */
@Component
public class SnowflakeIdGenerator implements TimeBasedIdGenerator {

    static final long MAX_ROLLBACK_MILLIS = 5;
    private static final int WORKER_BITS = 10;
    private static final int SEQUENCE_BITS = 12;
    private static final long MAX_WORKER = (1L << WORKER_BITS) - 1;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;

    private final Clock clock;
    private final long epochMillis;
    private final long workerId;

    private long lastTimestamp = -1L;
    private long sequence;

    public SnowflakeIdGenerator(SnowflakeProperties properties, Clock clock) {
        if (properties.workerId() < 0 || properties.workerId() > MAX_WORKER) {
            throw new IllegalStateException("identity-access.snowflake.worker-id must be between 0 and " + MAX_WORKER);
        }
        this.clock = clock;
        this.epochMillis = properties.epoch().toEpochMilli();
        this.workerId = properties.workerId();
    }

    @Override
    public synchronized long nextId() {
        long timestamp = clock.millis();
        if (timestamp < lastTimestamp) {
            if (lastTimestamp - timestamp > MAX_ROLLBACK_MILLIS) {
                throw new IllegalStateException("Clock moved backwards by more than " + MAX_ROLLBACK_MILLIS + " ms");
            }
            timestamp = waitUntilAtLeast(lastTimestamp);
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitUntilAtLeast(lastTimestamp + 1);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        long elapsed = timestamp - epochMillis;
        if (elapsed < 0) {
            throw new IllegalStateException("Clock is earlier than the configured snowflake epoch");
        }
        return (elapsed << (WORKER_BITS + SEQUENCE_BITS)) | (workerId << SEQUENCE_BITS) | sequence;
    }

    private long waitUntilAtLeast(long target) {
        long now = clock.millis();
        while (now < target) {
            Thread.onSpinWait();
            now = clock.millis();
        }
        return now;
    }
}
