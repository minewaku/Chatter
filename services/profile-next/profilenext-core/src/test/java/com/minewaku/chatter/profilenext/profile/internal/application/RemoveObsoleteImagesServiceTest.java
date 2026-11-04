package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.minewaku.chatter.profilenext.profile.api.command.RemoveObsoleteImagesUseCase;
import com.minewaku.chatter.profilenext.profile.internal.application.command.RemovalPolicy;
import com.minewaku.chatter.profilenext.profile.internal.application.command.RemoveObsoleteImagesService;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeProfileStore;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeRemovalQueue;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class RemoveObsoleteImagesServiceTest {

    private static final Instant START = Instant.parse("2026-11-04T10:00:00Z");
    private static final String FILE = "chatter/profile/avatars/7/old";

    private final AtomicReference<Instant> now = new AtomicReference<>(START);
    private final FakeImageStorage storage = new FakeImageStorage();
    private final FakeRemovalQueue queue = new FakeRemovalQueue();
    private final RemovalPolicy policy = new RemovalPolicy(
            10,
            List.of(
                    Duration.ofMinutes(2),
                    Duration.ofMinutes(4),
                    Duration.ofMinutes(8),
                    Duration.ofMinutes(16),
                    Duration.ofMinutes(32),
                    Duration.ofMinutes(60)),
            50,
            Duration.ofMinutes(5));
    private final RemoveObsoleteImagesService service = new RemoveObsoleteImagesService(
            storage, queue, policy, new java.time.Clock() {
                @Override
                public java.time.ZoneId getZone() {
                    return java.time.ZoneOffset.UTC;
                }

                @Override
                public java.time.Clock withZone(java.time.ZoneId zone) {
                    return this;
                }

                @Override
                public Instant instant() {
                    return now.get();
                }
            });

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void captureLogs() {
        logger = (Logger) LoggerFactory.getLogger(RemoveObsoleteImagesService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void releaseLogs() {
        logger.detachAppender(appender);
    }

    private RemoveObsoleteImagesUseCase.Result run() {
        return service.handle(new RemoveObsoleteImagesUseCase.Command());
    }

    @Test
    void aDueRowWhoseRemovalSucceedsIsDeletedFromTheQueue() {
        queue.enqueue(FILE, ImageSlot.AVATAR, START);

        var result = run();

        assertThat(result.removed()).isEqualTo(1);
        assertThat(storage.removed).containsExactly(FILE);
        assertThat(queue.rows).isEmpty();
    }

    @Test
    void aFailureCountsAnAttemptAndSchedulesTheNextOneByTheDelays() {
        storage.failRemoval = true;
        queue.enqueue(FILE, ImageSlot.AVATAR, START);

        var result = run();

        assertThat(result.failed()).isEqualTo(1);
        var row = queue.rows.get(FILE);
        assertThat(row.attempts).isEqualTo(1);
        assertThat(row.nextAttemptAt).isEqualTo(START.plus(Duration.ofMinutes(2)));
        assertThat(row.reportedAt).isNull();

        now.set(START.plus(Duration.ofMinutes(2)));
        run();
        assertThat(queue.rows.get(FILE).attempts).isEqualTo(2);
        assertThat(queue.rows.get(FILE).nextAttemptAt)
                .isEqualTo(START.plus(Duration.ofMinutes(2)).plus(Duration.ofMinutes(4)));
    }

    @Test
    void aRowNotYetDueIsNotTouched() {
        storage.failRemoval = true;
        queue.enqueue(FILE, ImageSlot.AVATAR, START);
        run();

        now.set(START.plus(Duration.ofMinutes(1)));
        run();

        assertThat(storage.removeCalls).hasSize(1);
    }

    @Test
    void afterTheTenthFailedRetryTheRowIsReportedOnceAndNeverRetriedAgain() {
        storage.failRemoval = true;
        queue.enqueue(FILE, ImageSlot.AVATAR, START);
        Duration waited = Duration.ZERO;

        // first attempt, then ten retries, each at the moment the previous failure said it was due
        for (int attempt = 0; attempt <= 10; attempt++) {
            now.set(START.plus(waited));
            run();
            if (attempt < 10) {
                Duration delay = Duration.between(now.get(), queue.rows.get(FILE).nextAttemptAt);
                waited = waited.plus(delay);
            }
        }

        // the delays of the ten retries add up to 362 minutes
        assertThat(waited).isEqualTo(Duration.ofMinutes(362));
        var row = queue.rows.get(FILE);
        assertThat(row.attempts).isEqualTo(11);
        assertThat(row.reportedAt).isNotNull();

        List<ILoggingEvent> errors = appender.list.stream()
                .filter(event -> event.getLevel() == Level.ERROR)
                .toList();
        assertThat(errors).hasSize(1);
        String message = errors.get(0).getFormattedMessage();
        assertThat(message).contains(FILE).contains("11");
        assertThat(Pattern.compile("(?i)password|hash|token|secret|credential|privateKey")
                        .matcher(message)
                        .find())
                .isFalse();

        // later runs skip it
        int callsBefore = storage.removeCalls.size();
        now.set(START.plus(Duration.ofDays(2)));
        run();
        assertThat(storage.removeCalls).hasSize(callsBefore);
    }

    @Test
    void aFailureNeverTouchesAnyProfile() {
        FakeProfileStore profiles = new FakeProfileStore();
        profiles.replaceImage(
                com.minewaku.chatter.profilenext.profile.internal.model.UserId.of(7),
                ImageSlot.AVATAR,
                new com.minewaku.chatter.profilenext.profile.internal.model.StoredImage(
                        "chatter/profile/avatars/7/new", "https://img/new"),
                START);
        storage.failRemoval = true;
        queue.enqueue(FILE, ImageSlot.AVATAR, START);

        run();

        // the service has no way to reach a profile; the stored image is exactly what it was
        assertThat(profiles.rows.get(7L).avatar().publicId()).isEqualTo("chatter/profile/avatars/7/new");
    }
}
