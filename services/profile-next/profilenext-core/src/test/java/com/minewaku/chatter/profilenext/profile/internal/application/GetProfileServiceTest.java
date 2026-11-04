package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.profile.api.query.GetProfileUseCase;
import com.minewaku.chatter.profilenext.profile.internal.application.query.GetProfileService;
import com.minewaku.chatter.profilenext.profile.internal.model.Profile;
import com.minewaku.chatter.profilenext.profile.internal.model.StoredImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeProfileStore;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GetProfileServiceTest {

    static class InMemoryQueries extends FakeProfileStore {}

    private final InMemoryQueries queries = new InMemoryQueries();
    private final GetProfileService service = new GetProfileService(queries);

    @Test
    void returnsTheSavedValuesWhenAProfileExists() {
        Instant created = Instant.parse("2026-11-01T10:00:00Z");
        Instant updated = Instant.parse("2026-11-02T10:00:00Z");
        queries.rows.put(
                7L,
                new Profile(
                        UserId.of(7),
                        "Mine",
                        "Hello",
                        new StoredImage("chatter/profile/avatars/7/a", "https://img/a"),
                        new StoredImage("chatter/profile/banners/7/b", "https://img/b"),
                        created,
                        updated));

        GetProfileUseCase.Result result = service.handle(new GetProfileUseCase.Query(7));

        assertThat(result.userId()).isEqualTo(7);
        assertThat(result.displayName()).isEqualTo("Mine");
        assertThat(result.bio()).isEqualTo("Hello");
        assertThat(result.avatarUrl()).isEqualTo("https://img/a");
        assertThat(result.bannerUrl()).isEqualTo("https://img/b");
        assertThat(result.createdAt()).isEqualTo(created);
        assertThat(result.updatedAt()).isEqualTo(updated);
    }

    @Test
    void returnsDefaultsWhenNothingWasEverSaved() {
        GetProfileUseCase.Result result = service.handle(new GetProfileUseCase.Query(42));

        assertThat(result.userId()).isEqualTo(42);
        assertThat(result.displayName()).isEmpty();
        assertThat(result.bio()).isEmpty();
        assertThat(result.avatarUrl()).isNull();
        assertThat(result.bannerUrl()).isNull();
        assertThat(result.createdAt()).isNull();
        assertThat(result.updatedAt()).isNull();
    }
}
