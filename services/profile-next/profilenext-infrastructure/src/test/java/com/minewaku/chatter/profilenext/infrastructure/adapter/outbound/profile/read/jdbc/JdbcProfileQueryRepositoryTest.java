package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.profile.read.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.infrastructure.support.JdbcTestConfiguration;
import com.minewaku.chatter.profilenext.infrastructure.support.PostgreSqlTestSupport;
import com.minewaku.chatter.profilenext.profile.internal.model.Bio;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.Profile;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.query.ProfileQueryRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(JdbcTestConfiguration.class)
class JdbcProfileQueryRepositoryTest extends PostgreSqlTestSupport {

    @Autowired
    private ProfileQueryRepository queries;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void anUnknownUserHasNoRow() {
        assertThat(queries.findByUserId(UserId.of(9_000_001L))).isEmpty();
    }

    @Test
    void mapsEveryColumnOfAStoredRow() {
        Instant created = Instant.parse("2026-11-01T10:00:00Z");
        Instant updated = Instant.parse("2026-11-02T11:30:00Z");
        jdbc.sql("""
                        INSERT INTO profile (user_id, display_name, bio, avatar_public_id, avatar_url,
                                             banner_public_id, banner_url, created_at, updated_at)
                        VALUES (9000002, 'Mine', 'Hello', 'chatter/profile/avatars/9000002/a', 'https://img/a',
                                'chatter/profile/banners/9000002/b', 'https://img/b', :created, :updated)
                        """)
                .param("created", java.sql.Timestamp.from(created))
                .param("updated", java.sql.Timestamp.from(updated))
                .update();

        Optional<Profile> found = queries.findByUserId(UserId.of(9_000_002L));

        assertThat(found).isPresent();
        Profile profile = found.get();
        assertThat(profile.userId().value()).isEqualTo(9_000_002L);
        assertThat(profile.displayName()).isEqualTo(DisplayName.of("Mine"));
        assertThat(profile.bio()).isEqualTo(Bio.of("Hello"));
        assertThat(profile.avatar().publicId()).isEqualTo("chatter/profile/avatars/9000002/a");
        assertThat(profile.avatar().url()).isEqualTo("https://img/a");
        assertThat(profile.banner().publicId()).isEqualTo("chatter/profile/banners/9000002/b");
        assertThat(profile.banner().url()).isEqualTo("https://img/b");
        assertThat(profile.createdAt()).isEqualTo(created);
        assertThat(profile.updatedAt()).isEqualTo(updated);
    }

    @Test
    void aRowWithoutImagesHasNoImageObjects() {
        jdbc.sql("INSERT INTO profile (user_id, created_at, updated_at) VALUES (9000003, now(), now())")
                .update();

        Profile profile = queries.findByUserId(UserId.of(9_000_003L)).orElseThrow();

        assertThat(profile.avatar()).isNull();
        assertThat(profile.banner()).isNull();
        assertThat(profile.displayName()).isNull();
        assertThat(profile.bio()).isEqualTo(Bio.of(""));
    }

    @Test
    void findReferencedPublicIdsReturnsTheSubsetAnyProfileHoldsInEitherSlot() {
        jdbc.sql("""
                        INSERT INTO profile (user_id, avatar_public_id, avatar_url, banner_public_id, banner_url,
                                             created_at, updated_at)
                        VALUES (9000010, 'ref/avatar', 'https://img/a', 'ref/banner', 'https://img/b', now(), now())
                        """)
                .update();

        assertThat(queries.findReferencedPublicIds(java.util.List.of("ref/avatar", "ref/banner", "ref/unused")))
                .containsExactlyInAnyOrder("ref/avatar", "ref/banner");
        assertThat(queries.findReferencedPublicIds(java.util.List.of("ref/unused"))).isEmpty();
        assertThat(queries.findReferencedPublicIds(java.util.List.of())).isEmpty();
    }
}
