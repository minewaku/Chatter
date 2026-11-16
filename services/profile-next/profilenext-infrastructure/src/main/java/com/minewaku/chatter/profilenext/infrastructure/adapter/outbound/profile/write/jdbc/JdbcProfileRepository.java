package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.profile.write.jdbc;

import com.minewaku.chatter.profilenext.profile.internal.model.Bio;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.ProfileRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
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

    @Override
    public Optional<ImageId> currentImage(UserId userId, ImageSlot slot) {
        return jdbc.sql("SELECT " + column(slot) + " FROM profile WHERE user_id = :userId")
                .param("userId", userId.value())
                .query((rs, rowNum) -> Optional.ofNullable(rs.getObject(1, UUID.class)).map(ImageId::new))
                .optional()
                .flatMap(image -> image);
    }

    /**
     * Makes sure the row exists, locks it, and sets the slot; the lock is what serialises two commits of one user.
     * Returns what the slot held before.
     */
    @Override
    @Transactional
    public Optional<ImageId> replaceImage(UserId userId, ImageSlot slot, ImageId imageId, Instant now) {
        OffsetDateTime at = OffsetDateTime.ofInstant(now, ZoneOffset.UTC);
        jdbc.sql("""
                        INSERT INTO profile (user_id, created_at, updated_at) VALUES (:userId, :now, :now)
                        ON CONFLICT (user_id) DO NOTHING
                        """)
                .param("userId", userId.value())
                .param("now", at)
                .update();
        Optional<ImageId> previous = lockedImage(userId, slot);
        jdbc.sql("UPDATE profile SET " + column(slot) + " = :imageId, updated_at = :now WHERE user_id = :userId")
                .param("imageId", imageId.value())
                .param("now", at)
                .param("userId", userId.value())
                .update();
        return previous;
    }

    @Override
    @Transactional
    public Optional<ImageId> removeImage(UserId userId, ImageSlot slot, Instant now) {
        Optional<ImageId> previous = lockedImage(userId, slot);
        if (previous.isPresent()) {
            jdbc.sql("UPDATE profile SET " + column(slot) + " = NULL, updated_at = :now WHERE user_id = :userId")
                    .param("now", OffsetDateTime.ofInstant(now, ZoneOffset.UTC))
                    .param("userId", userId.value())
                    .update();
        }
        return previous;
    }

    private Optional<ImageId> lockedImage(UserId userId, ImageSlot slot) {
        return jdbc.sql("SELECT " + column(slot) + " FROM profile WHERE user_id = :userId FOR UPDATE")
                .param("userId", userId.value())
                .query((rs, rowNum) -> Optional.ofNullable(rs.getObject(1, UUID.class)).map(ImageId::new))
                .optional()
                .flatMap(image -> image);
    }

    /** One of two fixed column names, never request text. */
    private static String column(ImageSlot slot) {
        return slot == ImageSlot.AVATAR ? "avatar_image_id" : "banner_image_id";
    }

    @Override
    public Optional<DeletedProfile> deleteByUserId(UserId userId) {
        return jdbc.sql("""
                        DELETE FROM profile WHERE user_id = :userId
                        RETURNING avatar_image_id, banner_image_id
                        """)
                .param("userId", userId.value())
                .query((rs, rowNum) -> new DeletedProfile(
                        imageId(rs.getObject("avatar_image_id", UUID.class)),
                        imageId(rs.getObject("banner_image_id", UUID.class))))
                .optional();
    }

    private static ImageId imageId(UUID value) {
        return value == null ? null : new ImageId(value);
    }
}
