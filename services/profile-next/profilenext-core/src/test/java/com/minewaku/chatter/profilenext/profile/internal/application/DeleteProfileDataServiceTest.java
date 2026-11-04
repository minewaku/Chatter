package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.profile.api.command.DeleteProfileDataUseCase;
import com.minewaku.chatter.profilenext.profile.internal.application.command.DeleteProfileDataService;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeProfileStore;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeRemovalQueue;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.StoredImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class DeleteProfileDataServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    private final FakeProfileStore profiles = new FakeProfileStore();
    private final FakeRemovalQueue removals = new FakeRemovalQueue();
    private final DeleteProfileDataService service =
            new DeleteProfileDataService(profiles, removals, Clock.fixed(NOW, ZoneOffset.UTC));

    private static DeleteProfileDataUseCase.Command notice(long userId) {
        return new DeleteProfileDataUseCase.Command("0197-event", userId, NOW);
    }

    private void giveBothImages(long userId) {
        UserId id = UserId.of(userId);
        profiles.applyEdit(id, DisplayName.of("Name " + userId), null, NOW);
        profiles.replaceImage(
                id,
                ImageSlot.AVATAR,
                new StoredImage("chatter/profile/avatars/" + userId + "/a", "https://img/a" + userId),
                NOW);
        profiles.replaceImage(
                id,
                ImageSlot.BANNER,
                new StoredImage("chatter/profile/banners/" + userId + "/b", "https://img/b" + userId),
                NOW);
    }

    @Test
    void deletesTheRowAndQueuesBothImages() {
        giveBothImages(7);

        service.handle(notice(7));

        assertThat(profiles.rows).doesNotContainKey(7L);
        assertThat(removals.rows).containsOnlyKeys("chatter/profile/avatars/7/a", "chatter/profile/banners/7/b");
    }

    @Test
    void aUserWithNoProfileSucceedsAndQueuesNothing() {
        service.handle(notice(8));

        assertThat(profiles.rows).isEmpty();
        assertThat(removals.rows).isEmpty();
    }

    @Test
    void aUserWithTextOnlyQueuesNothing() {
        profiles.applyEdit(UserId.of(9), DisplayName.of("Nine"), null, NOW);

        service.handle(notice(9));

        assertThat(profiles.rows).isEmpty();
        assertThat(removals.rows).isEmpty();
    }

    @Test
    void theSameCommandTwiceLeavesTheSameEndState() {
        giveBothImages(7);

        service.handle(notice(7));
        service.handle(notice(7));

        assertThat(profiles.rows).isEmpty();
        assertThat(removals.rows).containsOnlyKeys("chatter/profile/avatars/7/a", "chatter/profile/banners/7/b");
    }

    @Test
    void removesOnlyThatUsersImages() {
        giveBothImages(7);
        giveBothImages(8);

        service.handle(notice(7));

        assertThat(profiles.rows).containsOnlyKeys(8L);
        assertThat(profiles.rows.get(8L).avatar().publicId()).isEqualTo("chatter/profile/avatars/8/a");
        assertThat(removals.rows.keySet()).allMatch(id -> id.contains("/7/"));
    }
}
