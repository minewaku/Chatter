package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.profile.api.query.GetProfileUseCase;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeProfileStore;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakePromotionQueue;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.ImageIds;
import com.minewaku.chatter.profilenext.profile.internal.application.query.GetProfileService;
import com.minewaku.chatter.profilenext.profile.internal.model.Bio;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.Profile;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GetProfileServiceTest {

    private final FakePromotionQueue promotions = new FakePromotionQueue();
    private final FakeProfileStore queries = new FakeProfileStore(promotions);
    private final GetProfileService service = new GetProfileService(queries);

    @Test
    void returnsTheSavedValuesWhenAProfileExists() {
        Instant created = Instant.parse("2026-11-01T10:00:00Z");
        Instant updated = Instant.parse("2026-11-02T10:00:00Z");
        queries.rows.put(
                7L,
                new Profile(
                        UserId.of(7),
                        DisplayName.of("Mine"),
                        Bio.of("Hello"),
                        ImageIds.at(1),
                        ImageIds.at(2),
                        created,
                        updated));

        GetProfileUseCase.Result result = service.handle(new GetProfileUseCase.Query(7));

        assertThat(result.userId()).isEqualTo(7);
        assertThat(result.displayName()).isEqualTo("Mine");
        assertThat(result.bio()).isEqualTo("Hello");
        assertThat(result.avatar())
                .isEqualTo(new GetProfileUseCase.ImageResult(ImageIds.at(1).value().toString(), true));
        assertThat(result.banner())
                .isEqualTo(new GetProfileUseCase.ImageResult(ImageIds.at(2).value().toString(), true));
        assertThat(result.createdAt()).isEqualTo(created);
        assertThat(result.updatedAt()).isEqualTo(updated);
    }

    @Test
    void aPendingPromotionMakesTheImageNotPromoted() {
        Instant now = Instant.parse("2026-11-02T10:00:00Z");
        queries.rows.put(7L, new Profile(UserId.of(7), null, Bio.of(""), ImageIds.at(1), null, now, now));
        promotions.enqueue(new OwnedImage(UserId.of(7), ImageSlot.AVATAR, ImageIds.at(1)), now);

        GetProfileUseCase.Result result = service.handle(new GetProfileUseCase.Query(7));

        assertThat(result.avatar().promoted()).isFalse();
        assertThat(result.banner()).isNull();
    }

    @Test
    void returnsDefaultsWhenNothingWasEverSaved() {
        GetProfileUseCase.Result result = service.handle(new GetProfileUseCase.Query(42));

        assertThat(result.userId()).isEqualTo(42);
        assertThat(result.displayName()).isEmpty();
        assertThat(result.bio()).isEmpty();
        assertThat(result.avatar()).isNull();
        assertThat(result.banner()).isNull();
        assertThat(result.createdAt()).isNull();
        assertThat(result.updatedAt()).isNull();
    }

    @Test
    void thePromotedFlagFlipsWhenThePromotionRecordIsDeleted() {
        Instant now = Instant.parse("2026-11-02T10:00:00Z");
        queries.rows.put(7L, new Profile(UserId.of(7), null, Bio.of(""), ImageIds.at(1), null, now, now));
        promotions.enqueue(new OwnedImage(UserId.of(7), ImageSlot.AVATAR, ImageIds.at(1)), now);
        assertThat(service.handle(new GetProfileUseCase.Query(7)).avatar().promoted()).isFalse();

        promotions.markPromoted(ImageIds.at(1));

        assertThat(service.handle(new GetProfileUseCase.Query(7)).avatar().promoted()).isTrue();
    }
}
