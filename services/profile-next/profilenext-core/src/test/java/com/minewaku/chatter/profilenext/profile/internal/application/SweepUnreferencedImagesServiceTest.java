package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.profile.api.command.SweepUnreferencedImagesUseCase;
import com.minewaku.chatter.profilenext.profile.internal.application.command.SweepPolicy;
import com.minewaku.chatter.profilenext.profile.internal.application.command.SweepUnreferencedImagesService;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeProfileStore;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeRemovalQueue;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.StoredImage;
import com.minewaku.chatter.profilenext.profile.internal.model.StoredImageInfo;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SweepUnreferencedImagesServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");
    private static final Instant OLD = NOW.minus(Duration.ofHours(30));
    private static final Instant YOUNG = NOW.minus(Duration.ofHours(2));

    private final FakeImageStorage storage = new FakeImageStorage();
    private final FakeProfileStore profiles = new FakeProfileStore();
    private final FakeRemovalQueue queue = new FakeRemovalQueue();
    private final SweepUnreferencedImagesService service = new SweepUnreferencedImagesService(
            storage, profiles, queue, new SweepPolicy(Duration.ofHours(24)), Clock.fixed(NOW, ZoneOffset.UTC));

    private static StoredImageInfo info(String name, Instant createdAt) {
        return new StoredImageInfo("chatter/profile/avatars/7/" + name, createdAt);
    }

    private SweepUnreferencedImagesUseCase.Result run() {
        return service.handle(new SweepUnreferencedImagesUseCase.Command());
    }

    @Test
    void removesOldUnreferencedUnqueuedFilesOnly() {
        profiles.replaceImage(
                UserId.of(7),
                ImageSlot.AVATAR,
                new StoredImage("chatter/profile/avatars/7/used", "https://img/used"),
                NOW);
        queue.enqueue("chatter/profile/avatars/7/queued", ImageSlot.AVATAR, NOW);
        storage.pages.add(FakeImageStorage.page(
                null,
                info("stray", OLD),
                info("used", OLD),
                info("young", YOUNG),
                info("queued", OLD)));

        var result = run();

        assertThat(result.removed()).isEqualTo(1);
        assertThat(storage.removed).containsExactly("chatter/profile/avatars/7/stray");
    }

    @Test
    void aFileWhoseQueueRowIsReportedIsRemovedAndTheRowCleared() {
        String id = "chatter/profile/avatars/7/given-up";
        queue.enqueue(id, ImageSlot.AVATAR, OLD);
        queue.rows.get(id).reportedAt = OLD;
        storage.pages.add(FakeImageStorage.page(null, info("given-up", OLD)));

        var result = run();

        assertThat(result.removed()).isEqualTo(1);
        assertThat(storage.removed).containsExactly(id);
        assertThat(queue.rows).isEmpty();
    }

    @Test
    void followsTheCursorAcrossPages() {
        storage.pages.add(FakeImageStorage.page("1", info("a", OLD)));
        storage.pages.add(FakeImageStorage.page(null, info("b", OLD)));

        var result = run();

        assertThat(result.removed()).isEqualTo(2);
        assertThat(storage.listCursors).containsExactly(null, "1");
    }

    @Test
    void stopsCleanlyWhenTheProviderReportsARateLimit() {
        storage.pages.add(FakeImageStorage.page("1", info("a", OLD)));
        storage.pages.add(FakeImageStorage.page(null, info("b", OLD)));
        storage.listsBeforeRateLimit = 1;

        var result = run();

        assertThat(result.removed()).isEqualTo(1);
        assertThat(storage.removed).containsExactly("chatter/profile/avatars/7/a");
    }
}
