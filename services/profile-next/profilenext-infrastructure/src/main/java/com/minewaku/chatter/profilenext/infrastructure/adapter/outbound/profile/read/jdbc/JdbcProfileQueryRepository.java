package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.profile.read.jdbc;

import com.minewaku.chatter.profilenext.profile.internal.model.Bio;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.Profile;
import com.minewaku.chatter.profilenext.profile.internal.model.StoredImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.query.ProfileQueryRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class JdbcProfileQueryRepository implements ProfileQueryRepository {

    private final JdbcClient jdbc;

    JdbcProfileQueryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Profile> findByUserId(UserId userId) {
        return jdbc.sql("""
                        SELECT user_id, display_name, bio, avatar_public_id, avatar_url,
                               banner_public_id, banner_url, created_at, updated_at
                        FROM profile
                        WHERE user_id = :userId
                        """)
                .param("userId", userId.value())
                .query(JdbcProfileQueryRepository::map)
                .optional();
    }

    @Override
    public Set<String> findReferencedPublicIds(Collection<String> publicIds) {
        if (publicIds.isEmpty()) {
            return Set.of();
        }
        Set<String> referenced = new HashSet<>(jdbc.sql("""
                        SELECT avatar_public_id FROM profile WHERE avatar_public_id IN (:ids)
                        UNION
                        SELECT banner_public_id FROM profile WHERE banner_public_id IN (:ids)
                        """)
                .param("ids", publicIds)
                .query(String.class)
                .list());
        return referenced;
    }

    private static Profile map(ResultSet rs, int rowNum) throws SQLException {
        return new Profile(
                UserId.of(rs.getLong("user_id")),
                displayName(rs.getString("display_name")),
                Bio.of(rs.getString("bio")),
                image(rs.getString("avatar_public_id"), rs.getString("avatar_url")),
                image(rs.getString("banner_public_id"), rs.getString("banner_url")),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }

    private static DisplayName displayName(String stored) {
        return stored == null || stored.isBlank() ? null : DisplayName.of(stored);
    }

    private static StoredImage image(String publicId, String url) {
        return publicId == null || url == null ? null : new StoredImage(publicId, url);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
