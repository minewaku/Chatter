package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.shared.id;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7IdGeneratorTest {

    private final UuidV7IdGenerator generator = new UuidV7IdGenerator();

    @Test
    void producesVersionSevenUuidStrings() {
        String id = generator.nextId();

        assertThat(id).hasSize(36);
        assertThat(UUID.fromString(id).version()).isEqualTo(7);
    }

    @Test
    void tenThousandIdsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            ids.add(generator.nextId());
        }

        assertThat(ids).hasSize(10_000);
    }

    @Test
    void idsGeneratedLaterSortAfterEarlierOnes() throws InterruptedException {
        String earlier = generator.nextId();
        Thread.sleep(5);
        String later = generator.nextId();

        assertThat(later).isGreaterThan(earlier);
    }
}
