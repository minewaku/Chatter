package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.profile.api.command.DeleteProfileDataUseCase;
import com.minewaku.chatter.profilenext.profile.internal.application.command.DeleteProfileDataService;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeProfileStore;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakePromotionQueue;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.ImageIds;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeRemovalQueue;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class DeleteProfileDataServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    private final FakePromotionQueue promotions = new FakePromotionQueue();
    private final FakeProfileStore profiles = new FakeProfileStore(promotions);
    private final FakeRemovalQueue removals = new FakeRemovalQueue();
    private final DeleteProfileDataService service =
            new DeleteProfileDataService(profiles, removals, promotions, Clock.fixed(NOW, ZoneOffset.UTC));

    private static OwnedImage avatar(long user) {
        return new OwnedImage(UserId.of(user), ImageSlot.AVATAR, ImageIds.at((int) user * 10 + 1));
    }

    private static OwnedImage banner(long user) {
        return new OwnedImage(UserId.of(user), ImageSlot.BANNER, ImageIds.at((int) user * 10 + 2));
    }

    private static DeleteProfileDataUseCase.Command notice(long userId) {
        return new DeleteProfileDataUseCase.Command("0197-event", userId, NOW);
    }

    private void giveBothImages(long userId) {
        UserId id = UserId.of(userId);
        profiles.applyEdit(id, DisplayName.of("Name " + userId), null, NOW);
        profiles.setImage(id, ImageSlot.AVATAR, avatar(userId).imageId(), NOW);
        profiles.setImage(id, ImageSlot.BANNER, banner(userId).imageId(), NOW);
        promotions.enqueue(avatar(userId), NOW);
        promotions.enqueue(banner(userId), NOW);
    }

    @Test
    void deletesTheRowAndQueuesBothImages() {
        giveBothImages(7);

        service.handle(notice(7));

        assertThat(profiles.rows).doesNotContainKey(7L);
        assertThat(removals.rows).containsOnlyKeys(avatar(7).imageId(), banner(7).imageId());
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
        assertThat(removals.rows).containsOnlyKeys(avatar(7).imageId(), banner(7).imageId());
    }

    @Test
    void removesOnlyThatUsersImages() {
        giveBothImages(7);
        giveBothImages(8);

        service.handle(notice(7));

        assertThat(profiles.rows).containsOnlyKeys(8L);
        assertThat(profiles.rows.get(8L).avatar()).isEqualTo(avatar(8).imageId());
        assertThat(removals.rows.values()).allMatch(row -> row.image.userId().equals(UserId.of(7)));
    }

    @Test
    void dropsThePendingPromotionsOfTheUserAndKeepsOthers() {
        giveBothImages(7);
        giveBothImages(8);

        service.handle(notice(7));

        assertThat(promotions.rows.values()).allMatch(row -> row.image.userId().equals(UserId.of(8)));
        assertThat(promotions.rows).hasSize(2);
    }

    @Test
    void aProfileWithOnlyABannerQueuesExactlyOneRemoval() {
        UserId id = UserId.of(7);
        profiles.applyEdit(id, DisplayName.of("Seven"), null, NOW);
        profiles.setImage(id, ImageSlot.BANNER, banner(7).imageId(), NOW);

        service.handle(notice(7));

        assertThat(removals.rows).containsOnlyKeys(banner(7).imageId());
        assertThat(removals.rows.get(banner(7).imageId()).image).isEqualTo(banner(7));
    }

    @Test
    void aStrayPromotionOfAUserWithNoProfileIsAlsoDropped() {
        promotions.enqueue(avatar(7), NOW);

        service.handle(notice(7));

        assertThat(promotions.rows).isEmpty();
        assertThat(removals.rows).isEmpty();
    }

    @Test
    void theWholeDeletionRunsInOneTransaction() {
        assertThat(DeleteProfileDataService.class.isAnnotationPresent(
                        org.springframework.transaction.annotation.Transactional.class))
                .isTrue();
    }
}
