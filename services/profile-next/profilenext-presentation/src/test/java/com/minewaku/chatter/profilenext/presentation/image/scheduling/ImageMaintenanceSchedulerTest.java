package com.minewaku.chatter.profilenext.presentation.image.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.minewaku.chatter.profilenext.profile.api.command.SweepPendingUploadsUseCase;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ImageMaintenanceSchedulerTest {

    @Test
    void aRunCallsTheSweepOnce() {
        AtomicInteger calls = new AtomicInteger();
        var scheduler = new ImageMaintenanceScheduler(command -> {
            calls.incrementAndGet();
            return new SweepPendingUploadsUseCase.Result(3, 0);
        });

        scheduler.sweepPendingUploads();

        assertThat(calls).hasValue(1);
    }

    @Test
    void aFailingSweepIsLoggedAndTheNextRunStillHappens() {
        AtomicInteger calls = new AtomicInteger();
        SweepPendingUploadsUseCase failing = command -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("database is down");
            }
            return new SweepPendingUploadsUseCase.Result(2, 0);
        };
        var scheduler = new ImageMaintenanceScheduler(failing);

        assertThatCode(scheduler::sweepPendingUploads).doesNotThrowAnyException();
        assertThatCode(scheduler::sweepPendingUploads).doesNotThrowAnyException();

        assertThat(calls).hasValue(2);
    }
}
