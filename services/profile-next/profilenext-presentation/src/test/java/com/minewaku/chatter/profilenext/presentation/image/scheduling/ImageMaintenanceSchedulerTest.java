package com.minewaku.chatter.profilenext.presentation.image.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.minewaku.chatter.profilenext.profile.api.command.RemoveObsoleteImagesUseCase;
import com.minewaku.chatter.profilenext.profile.api.command.SweepUnreferencedImagesUseCase;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ImageMaintenanceSchedulerTest {

    @Test
    void aFailingRemovalRunIsLoggedAndTheNextRunStillHappens() {
        AtomicInteger calls = new AtomicInteger();
        RemoveObsoleteImagesUseCase failing = command -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("database is down");
            }
            return new RemoveObsoleteImagesUseCase.Result(1, 0, 0);
        };
        var scheduler = new ImageMaintenanceScheduler(failing, command -> new SweepUnreferencedImagesUseCase.Result(0));

        assertThatCode(scheduler::drainRemovalQueue).doesNotThrowAnyException();
        assertThatCode(scheduler::drainRemovalQueue).doesNotThrowAnyException();

        assertThat(calls).hasValue(2);
    }

    @Test
    void aFailingSweepIsLoggedAndTheNextRunStillHappens() {
        AtomicInteger calls = new AtomicInteger();
        SweepUnreferencedImagesUseCase failing = command -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("provider down");
            }
            return new SweepUnreferencedImagesUseCase.Result(2);
        };
        var scheduler = new ImageMaintenanceScheduler(command -> new RemoveObsoleteImagesUseCase.Result(0, 0, 0), failing);

        assertThatCode(scheduler::sweepStrayImages).doesNotThrowAnyException();
        assertThatCode(scheduler::sweepStrayImages).doesNotThrowAnyException();

        assertThat(calls).hasValue(2);
    }
}
