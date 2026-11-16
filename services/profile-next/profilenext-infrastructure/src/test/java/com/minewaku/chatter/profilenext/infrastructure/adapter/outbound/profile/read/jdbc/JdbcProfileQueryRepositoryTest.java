package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.profile.read.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.infrastructure.support.JdbcTestConfiguration;
import com.minewaku.chatter.profilenext.infrastructure.support.PostgreSqlTestSupport;
import com.minewaku.chatter.profilenext.profile.internal.model.Bio;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.query.ProfileQueryRepository;
import com.minewaku.chatter.profilenext.profile.internal.port.query.ProfileQueryRepository.ProfileView;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
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
        UUID avatar = UUID.fromString("0197aaaa-0000-7000-8000-000000000001");
        UUID banner = UUID.fromString("0197aaab-0000-7000-8000-000000000001");
        jdbc.sql("""
                        INSERT INTO profile (user_id, display_name, bio, avatar_image_id, banner_image_id, created_at, updated_at)
                        VALUES (9000002, 'Mine', 'Hello', :avatar, :banner, :created, :updated)
                        """)
                .param("avatar", avatar)
                .param("banner", banner)
                .param("created", java.sql.Timestamp.from(created))
                .param("updated", java.sql.Timestamp.from(updated))
                .update();

        Optional<ProfileView> found = queries.findByUserId(UserId.of(9_000_002L));

        assertThat(found).isPresent();
        ProfileView profile = found.get();
        assertThat(profile.userId().value()).isEqualTo(9_000_002L);
        assertThat(profile.displayName()).isEqualTo(DisplayName.of("Mine"));
        assertThat(profile.bio()).isEqualTo(Bio.of("Hello"));
        assertThat(profile.avatar()).isEqualTo(new ImageId(avatar));
        assertThat(profile.banner()).isEqualTo(new ImageId(banner));
        assertThat(profile.createdAt()).isEqualTo(created);
        assertThat(profile.updatedAt()).isEqualTo(updated);
    }

    @Test
    void aRowWithoutImagesHasNoImages() {
        jdbc.sql("INSERT INTO profile (user_id, created_at, updated_at) VALUES (9000003, now(), now())")
                .update();

        ProfileView profile = queries.findByUserId(UserId.of(9_000_003L)).orElseThrow();

        assertThat(profile.avatar()).isNull();
        assertThat(profile.banner()).isNull();
        assertThat(profile.displayName()).isNull();
        assertThat(profile.bio()).isEqualTo(Bio.of(""));
    }
}
