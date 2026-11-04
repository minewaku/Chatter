package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.profile.write.jdbc;

import com.minewaku.chatter.profilenext.profile.internal.model.Bio;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.StoredImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.ProfileRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JdbcProfileRepository implements ProfileRepository {

    private final JdbcClient jdbc;

    JdbcProfileRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * One statement does the whole save: the unique key arbitrates the first insert, Postgres serialises concurrent
     * updates on the row, and the {@code IS DISTINCT FROM} guard leaves an unchanged row (and its {@code updated_at})
     * alone.
     */
    @Override
    public boolean applyEdit(UserId userId, DisplayName displayName, Bio bio, Instant now) {
        int written = jdbc.sql("""
                        INSERT INTO profile (user_id, display_name, bio, created_at, updated_at)
                        VALUES (:userId, COALESCE(CAST(:displayName AS text), ''), COALESCE(CAST(:bio AS text), ''), :now, :now)
                        ON CONFLICT (user_id) DO UPDATE
                        SET display_name = COALESCE(CAST(:displayName AS text), profile.display_name),
                            bio = COALESCE(CAST(:bio AS text), profile.bio),
                            updated_at = :now
                        WHERE (profile.display_name, profile.bio) IS DISTINCT FROM
                              (COALESCE(CAST(:displayName AS text), profile.display_name),
                               COALESCE(CAST(:bio AS text), profile.bio))
                        """)
                .param("userId", userId.value())
                .param("displayName", displayName == null ? null : displayName.value())
                .param("bio", bio == null ? null : bio.value())
                .param("now", OffsetDateTime.ofInstant(now, ZoneOffset.UTC))
                .update();
        return written > 0;
    }

    /**
     * Makes sure the row exists, locks it, and replaces the slot only when the new upload id is greater (ids are
     * time-ordered and compare as text). {@code updated_at} moves only when the slot changed.
     */
    @Override
    @Transactional
    public Replacement replaceImage(UserId userId, ImageSlot slot, StoredImage image, Instant now) {
        OffsetDateTime at = OffsetDateTime.ofInstant(now, ZoneOffset.UTC);
        String publicIdColumn = slot == ImageSlot.AVATAR ? "avatar_public_id" : "banner_public_id";
        String urlColumn = slot == ImageSlot.AVATAR ? "avatar_url" : "banner_url";

        jdbc.sql("""
                        INSERT INTO profile (user_id, created_at, updated_at) VALUES (:userId, :now, :now)
                        ON CONFLICT (user_id) DO NOTHING
                        """)
                .param("userId", userId.value())
                .param("now", at)
                .update();

        StoredImage current = jdbc.sql("SELECT " + publicIdColumn + ", " + urlColumn
                        + " FROM profile WHERE user_id = :userId FOR UPDATE")
                .param("userId", userId.value())
                .query((rs, rowNum) -> {
                    String publicId = rs.getString(1);
                    return Optional.ofNullable(publicId == null ? null : new StoredImage(publicId, rs.getString(2)));
                })
                .single()
                .orElse(null);

        if (current != null && image.uploadId().compareTo(current.uploadId()) <= 0) {
            return new Replacement(false, current);
        }
        jdbc.sql("UPDATE profile SET " + publicIdColumn + " = :publicId, " + urlColumn
                        + " = :url, updated_at = :now WHERE user_id = :userId")
                .param("publicId", image.publicId())
                .param("url", image.url())
                .param("now", at)
                .param("userId", userId.value())
                .update();
        return new Replacement(true, current);
    }

    @Override
    public Optional<DeletedProfile> deleteByUserId(UserId userId) {
        return jdbc.sql("""
                        DELETE FROM profile WHERE user_id = :userId
                        RETURNING avatar_public_id, avatar_url, banner_public_id, banner_url
                        """)
                .param("userId", userId.value())
                .query((rs, rowNum) -> new DeletedProfile(
                        rs.getString("avatar_public_id") == null
                                ? null
                                : new StoredImage(rs.getString("avatar_public_id"), rs.getString("avatar_url")),
                        rs.getString("banner_public_id") == null
                                ? null
                                : new StoredImage(rs.getString("banner_public_id"), rs.getString("banner_url"))))
                .optional();
    }
}
