package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.profile.read.jdbc;

import com.minewaku.chatter.profilenext.profile.internal.model.Bio;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.query.ProfileQueryRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class JdbcProfileQueryRepository implements ProfileQueryRepository {

    private final JdbcClient jdbc;

    JdbcProfileQueryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ProfileView> findByUserId(UserId userId) {
        return jdbc.sql("""
                        SELECT user_id, display_name, bio, avatar_image_id, banner_image_id, created_at, updated_at
                        FROM profile
                        WHERE user_id = :userId
                        """)
                .param("userId", userId.value())
                .query(JdbcProfileQueryRepository::map)
                .optional();
    }

    private static ProfileView map(ResultSet rs, int rowNum) throws SQLException {
        return new ProfileView(
                UserId.of(rs.getLong("user_id")),
                displayName(rs.getString("display_name")),
                Bio.of(rs.getString("bio")),
                imageId(rs.getObject("avatar_image_id", UUID.class)),
                imageId(rs.getObject("banner_image_id", UUID.class)),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }

    private static DisplayName displayName(String stored) {
        return stored == null || stored.isBlank() ? null : DisplayName.of(stored);
    }

    private static ImageId imageId(UUID value) {
        return value == null ? null : new ImageId(value);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
