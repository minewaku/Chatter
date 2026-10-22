package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.shared.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class SnowflakeIdGeneratorTest {

    private static SnowflakeIdGenerator generator(int worker, Clock clock) {
        return new SnowflakeIdGenerator(new SnowflakeProperties(worker, null), clock);
    }

    @Test
    void hundredThousandIdsAcrossEightThreadsHaveNoDuplicates() throws Exception {
        SnowflakeIdGenerator generator = generator(1, Clock.systemUTC());
        Set<Long> seen = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int t = 0; t < 8; t++) {
                Callable<Integer> task = () -> {
                    int duplicates = 0;
                    for (int i = 0; i < 12_500; i++) {
                        if (!seen.add(generator.nextId())) {
                            duplicates++;
                        }
                    }
                    return duplicates;
                };
                futures.add(pool.submit(task));
            }
            int duplicates = 0;
            for (Future<Integer> f : futures) {
                duplicates += f.get();
            }
            assertThat(duplicates).isZero();
            assertThat(seen).hasSize(100_000);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void idsFromOneThreadAreStrictlyIncreasingAndPositive() {
        SnowflakeIdGenerator generator = generator(3, Clock.systemUTC());
        long previous = 0;
        for (int i = 0; i < 20_000; i++) {
            long id = generator.nextId();
            assertThat(id).isGreaterThan(previous);
            previous = id;
        }
    }

    @Test
    void differentWorkersNeverEmitTheSameIdForTheSameInstant() {
        Clock fixed = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC);
        long a = generator(1, fixed).nextId();
        long b = generator(2, fixed).nextId();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void workerIdOutsideRangeStopsStartupAndNamesTheProperty() {
        assertThatThrownBy(() -> generator(1024, Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("identity-access.snowflake.worker-id");
        assertThatThrownBy(() -> generator(-1, Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("identity-access.snowflake.worker-id");
    }

    @Test
    void smallClockRollbackIsWaitedOutWithoutDuplicates() {
        long base = Instant.parse("2026-06-01T00:00:00Z").toEpochMilli();
        ScriptedClock clock = new ScriptedClock(base, base, base - 3, base - 2, base - 1, base, base + 1);
        SnowflakeIdGenerator generator = generator(1, clock);

        long first = generator.nextId();
        long second = generator.nextId();

        assertThat(second).isGreaterThan(first);
    }

    @Test
    void largeClockRollbackFails() {
        long base = Instant.parse("2026-06-01T00:00:00Z").toEpochMilli();
        ScriptedClock clock = new ScriptedClock(base, base - 50);
        SnowflakeIdGenerator generator = generator(1, clock);
        generator.nextId();

        assertThatThrownBy(generator::nextId).isInstanceOf(IllegalStateException.class);
    }

    /** Returns scripted millisecond values in order, repeating the last one once exhausted. */
    private static final class ScriptedClock extends Clock {
        private final long[] values;
        private int index;

        ScriptedClock(long... values) {
            this.values = values;
        }

        @Override
        public long millis() {
            long v = values[Math.min(index, values.length - 1)];
            index++;
            return v;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis());
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
