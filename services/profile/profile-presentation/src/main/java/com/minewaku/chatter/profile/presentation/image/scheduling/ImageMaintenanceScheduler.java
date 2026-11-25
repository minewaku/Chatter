package com.minewaku.chatter.profile.presentation.image.scheduling;

import com.minewaku.chatter.profile.profile.api.command.SweepPendingUploadsUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the upload sweep on a schedule. Every exception is caught and logged so one failure never stops the schedule; the
 * log line carries the exception, never a payload.
 */
@Component
@RequiredArgsConstructor
public class ImageMaintenanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(ImageMaintenanceScheduler.class);

    private final SweepPendingUploadsUseCase sweep;

    @Scheduled(
            fixedDelayString = "${profile.images.sweep.interval:PT1M}",
            initialDelayString = "${profile.images.sweep.interval:PT1M}")
    public void sweepPendingUploads() {
        try {
            var result = sweep.handle(new SweepPendingUploadsUseCase.Command());
            if (result.deleted() + result.failed() > 0) {
                log.info("image sweep deleted={} failed={}", result.deleted(), result.failed());
            }
        } catch (Exception e) {
            log.error("image sweep failed", e);
        }
    }
}
