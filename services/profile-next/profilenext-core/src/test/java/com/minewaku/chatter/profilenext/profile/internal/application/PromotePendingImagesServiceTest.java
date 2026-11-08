package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.minewaku.chatter.profilenext.profile.api.command.PromotePendingImagesUseCase;
import com.minewaku.chatter.profilenext.profile.internal.application.command.PromotePendingImagesService;
import com.minewaku.chatter.profilenext.profile.internal.application.command.PromotionPolicy;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeProfileStore;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakePromotionQueue;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.ImageIds;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class PromotePendingImagesServiceTest {

    private static final Instant START = Instant.parse("2026-11-04T10:00:00Z");

    private final AtomicReference<Instant> now = new AtomicReference<>(START);
    private final FakeImageStorage storage = new FakeImageStorage();
    private final FakePromotionQueue queue = new FakePromotionQueue();
    private final FakeProfileStore profiles = new FakeProfileStore(queue);
    private final PromotionPolicy policy = new PromotionPolicy(
            Duration.ofHours(2),
            List.of(Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(30)),
            10,
            Duration.ofMinutes(5));
    private final PromotePendingImagesService service =
            new PromotePendingImagesService(storage, queue, profiles, policy, new Clock() {
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
                    return now.get();
                }
            });

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void captureLogs() {
        logger = (Logger) LoggerFactory.getLogger(PromotePendingImagesService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void releaseLogs() {
        logger.detachAppender(appender);
    }

    private static OwnedImage avatar(long user, int n) {
        return new OwnedImage(UserId.of(user), ImageSlot.AVATAR, ImageIds.at(n));
    }

    /** The profile row of the user holds the image in its avatar slot, and the image waits for promotion. */
    private OwnedImage appliedImage(long user, int n) {
        OwnedImage image = avatar(user, n);
        profiles.applyEdit(UserId.of(user), null, null, START);
        profiles.setImage(image.userId(), ImageSlot.AVATAR, image.imageId(), START);
        queue.enqueue(image, START);
        return image;
    }

    private PromotePendingImagesUseCase.Result run() {
        return service.handle(new PromotePendingImagesUseCase.Command());
    }

    @Test
    void nothingDueDoesNothing() {
        var result = run();

        assertThat(result).isEqualTo(new PromotePendingImagesUseCase.Result(0, 0, 0));
        assertThat(storage.promoteCalls).isEmpty();
    }

    @Test
    void aSuccessfulPromotionDeletesTheRowAndTheImageReadsAsPromoted() {
        OwnedImage image = appliedImage(7, 1);
        assertThat(profiles.findByUserId(UserId.of(7)).orElseThrow().avatar().promoted()).isFalse();

        var result = run();

        assertThat(result).isEqualTo(new PromotePendingImagesUseCase.Result(1, 0, 0));
        assertThat(storage.promoteCalls).containsExactly(image);
        assertThat(queue.rows).isEmpty();
        assertThat(profiles.findByUserId(UserId.of(7)).orElseThrow().avatar().promoted()).isTrue();
    }

    @Test
    void failuresBeforeTheHorizonRescheduleWithGrowingDelaysThatStopAtTheLast() {
        OwnedImage image = appliedImage(7, 1);
        storage.failingPromotions.add(image.imageId());
        Duration[] expected = {Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(30), Duration.ofMinutes(30)};

        for (int attempt = 1; attempt <= 4; attempt++) {
            var result = run();

            assertThat(result).isEqualTo(new PromotePendingImagesUseCase.Result(0, 1, 0));
            var row = queue.rows.get(image.imageId());
            assertThat(row.attempts).isEqualTo(attempt);
            assertThat(row.nextAttemptAt).isEqualTo(now.get().plus(expected[attempt - 1]));
            assertThat(row.reportedAt).isNull();
            now.set(row.nextAttemptAt);
        }
    }

    @Test
    void aRowNotYetDueIsNotTouched() {
        OwnedImage image = appliedImage(7, 1);
        storage.failingPromotions.add(image.imageId());
        run();

        now.set(START.plusSeconds(30));
        var result = run();

        assertThat(result).isEqualTo(new PromotePendingImagesUseCase.Result(0, 0, 0));
        assertThat(storage.promoteCalls).hasSize(1);
    }

    @Test
    void aFailureAtTheHorizonClearsTheSlotReportsTheRowAndLogsAnError() {
        OwnedImage image = appliedImage(7, 1);
        storage.failingPromotions.add(image.imageId());
        now.set(START.plus(Duration.ofHours(2)));

        var result = run();

        assertThat(result).isEqualTo(new PromotePendingImagesUseCase.Result(0, 1, 1));
        assertThat(profiles.rows.get(7L).avatar()).isNull();
        assertThat(queue.rows.get(image.imageId()).reportedAt).isEqualTo(now.get());
        List<ILoggingEvent> errors = appender.list.stream()
                .filter(event -> event.getLevel() == Level.ERROR)
                .toList();
        assertThat(errors).hasSize(1);
        String message = errors.get(0).getFormattedMessage();
        assertThat(message).contains(image.imageId().value().toString());
        assertThat(Pattern.compile("(?i)password|hash|token|secret|credential|privateKey")
                        .matcher(message)
                        .find())
                .isFalse();

        now.set(START.plus(Duration.ofDays(2)));
        assertThat(run()).isEqualTo(new PromotePendingImagesUseCase.Result(0, 0, 0));
    }

    @Test
    void aFailureJustBeforeTheHorizonStillRetries() {
        OwnedImage image = appliedImage(7, 1);
        storage.failingPromotions.add(image.imageId());
        now.set(START.plus(Duration.ofHours(2)).minusSeconds(1));

        var result = run();

        assertThat(result).isEqualTo(new PromotePendingImagesUseCase.Result(0, 1, 0));
        assertThat(profiles.rows.get(7L).avatar()).isEqualTo(image.imageId());
    }

    @Test
    void givingUpAnOlderImageLeavesANewerOneInTheSlotAlone() {
        OwnedImage older = appliedImage(7, 1);
        profiles.setImage(UserId.of(7), ImageSlot.AVATAR, ImageIds.at(2), START);
        storage.failingPromotions.add(older.imageId());
        now.set(START.plus(Duration.ofHours(3)));

        var result = run();

        assertThat(result).isEqualTo(new PromotePendingImagesUseCase.Result(0, 1, 1));
        assertThat(profiles.rows.get(7L).avatar()).isEqualTo(ImageIds.at(2));
        assertThat(queue.rows.get(older.imageId()).reportedAt).isNotNull();
    }

    @Test
    void givingUpForAUserWithNoProfileCreatesNothingAndDoesNotFail() {
        OwnedImage image = avatar(7, 1);
        queue.enqueue(image, START);
        storage.failingPromotions.add(image.imageId());
        now.set(START.plus(Duration.ofHours(3)));

        var result = run();

        assertThat(result).isEqualTo(new PromotePendingImagesUseCase.Result(0, 1, 1));
        assertThat(profiles.rows).isEmpty();
        assertThat(queue.rows.get(image.imageId()).reportedAt).isNotNull();
    }

    @Test
    void aRunThatDiedBetweenClearingTheSlotAndReportingIsFinishedByTheNextOne() {
        OwnedImage image = appliedImage(7, 1);
        profiles.clearImageIf(UserId.of(7), ImageSlot.AVATAR, image.imageId(), START);
        storage.failingPromotions.add(image.imageId());
        now.set(START.plus(Duration.ofHours(3)));

        var result = run();

        assertThat(result).isEqualTo(new PromotePendingImagesUseCase.Result(0, 1, 1));
        assertThat(profiles.rows.get(7L).avatar()).isNull();
        assertThat(queue.rows.get(image.imageId()).reportedAt).isNotNull();
    }

    @Test
    void aLeasedRowIsNotTakenByASecondRunAtTheSameInstant() {
        OwnedImage image = appliedImage(7, 1);
        storage.failingPromotions.add(image.imageId());
        run();

        var second = run();

        assertThat(second).isEqualTo(new PromotePendingImagesUseCase.Result(0, 0, 0));
        assertThat(storage.promoteCalls).hasSize(1);
    }

    @Test
    void oneFailingImageDoesNotStopTheRestOfTheBatch() {
        OwnedImage first = appliedImage(7, 1);
        OwnedImage middle = appliedImage(8, 2);
        OwnedImage last = appliedImage(9, 3);
        storage.failingPromotions.add(middle.imageId());

        var result = run();

        assertThat(result).isEqualTo(new PromotePendingImagesUseCase.Result(2, 1, 0));
        assertThat(storage.promoteCalls).containsExactlyInAnyOrder(first, middle, last);
        assertThat(queue.rows).containsOnlyKeys(middle.imageId());
    }

    @Test
    void aRowIsDeletedWheneverPromoteReturnsNormallyEvenIfAnEarlierRunDidTheMove() {
        OwnedImage image = appliedImage(7, 1);
        run(); // the move happened and the row was deleted
        queue.enqueue(image, START); // simulate: the process died before the delete, so the row is still there
        now.set(START.plusSeconds(1));

        var result = run();

        assertThat(result).isEqualTo(new PromotePendingImagesUseCase.Result(1, 0, 0));
        assertThat(queue.rows).isEmpty();
    }
}
