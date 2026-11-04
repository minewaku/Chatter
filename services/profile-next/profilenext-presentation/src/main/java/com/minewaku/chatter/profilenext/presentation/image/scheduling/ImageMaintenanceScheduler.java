package com.minewaku.chatter.profilenext.presentation.image.scheduling;

import com.minewaku.chatter.profilenext.profile.api.command.RemoveObsoleteImagesUseCase;
import com.minewaku.chatter.profilenext.profile.api.command.SweepUnreferencedImagesUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the two image jobs on a schedule. Every exception is caught and logged so one failure never stops the
 * schedule; the log line carries the exception, never a payload.
 */
@Component
@RequiredArgsConstructor
public class ImageMaintenanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(ImageMaintenanceScheduler.class);

    private final RemoveObsoleteImagesUseCase removeObsolete;
    private final SweepUnreferencedImagesUseCase sweepUnreferenced;

    @Scheduled(
            fixedDelayString = "${profile-next.images.removal.poll-interval:PT30S}",
            initialDelayString = "${profile-next.images.removal.poll-interval:PT30S}")
    public void drainRemovalQueue() {
        try {
            var result = removeObsolete.handle(new RemoveObsoleteImagesUseCase.Command());
            if (result.removed() + result.failed() > 0) {
                log.info(
                        "image removal run removed={} failed={} reported={}",
                        result.removed(),
                        result.failed(),
                        result.reported());
            }
        } catch (Exception e) {
            log.error("image removal run failed", e);
        }
    }

    @Scheduled(
            fixedDelayString = "${profile-next.images.sweep.interval:PT1H}",
            initialDelayString = "${profile-next.images.sweep.interval:PT1H}")
    public void sweepStrayImages() {
        try {
            var result = sweepUnreferenced.handle(new SweepUnreferencedImagesUseCase.Command());
            if (result.removed() > 0) {
                log.info("image sweep removed={}", result.removed());
            }
        } catch (Exception e) {
            log.error("image sweep failed", e);
        }
    }
}
