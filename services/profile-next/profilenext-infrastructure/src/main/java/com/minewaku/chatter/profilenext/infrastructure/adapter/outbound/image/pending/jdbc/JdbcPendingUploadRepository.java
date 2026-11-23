package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.pending.jdbc;

import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.PendingUpload;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingUploadRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * The upload ledger in PostgreSQL. A row with an expiry is a pending ticket and one without is a key waiting to be
 * deleted; there is no limit on pending tickets per user. {@code lockNextDue} skips rows another worker holds, so sweepers on
 * several instances never share a row.
 */
@Component
public class JdbcPendingUploadRepository implements PendingUploadRepository {

    private static final String COLUMNS = "image_id, user_id, slot, expires_at, delete_after";

    private final JdbcClient jdbc;

    JdbcPendingUploadRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static OffsetDateTime at(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    @Override
    public Optional<PendingUpload> findPending(UserId userId, ImageId imageId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM pending_upload WHERE image_id = :imageId AND user_id = :userId AND expires_at IS NOT NULL")
                .param("imageId", imageId.value())
                .param("userId", userId.value())
                .query(JdbcPendingUploadRepository::map)
                .optional();
    }

    @Override
    public Optional<PendingUpload> lockPending(UserId userId, ImageId imageId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM pending_upload WHERE image_id = :imageId AND user_id = :userId AND expires_at IS NOT NULL FOR UPDATE")
                .param("imageId", imageId.value())
                .param("userId", userId.value())
                .query(JdbcPendingUploadRepository::map)
                .optional();
    }

    @Override
    public void insert(PendingUpload upload) {
        jdbc.sql("""
                        INSERT INTO pending_upload (image_id, user_id, slot, expires_at, delete_after, created_at)
                        VALUES (:imageId, :userId, :slot, :expiresAt, :deleteAfter, now())
                        """)
                .param("imageId", upload.image().imageId().value())
                .param("userId", upload.image().userId().value())
                .param("slot", upload.image().slot().name())
                .param("expiresAt", at(upload.expiresAt()))
                .param("deleteAfter", at(upload.deleteAfter()))
                .update();
    }

    @Override
    public void discard(OwnedImage image, Instant deleteAfter) {
        jdbc.sql("""
                        INSERT INTO pending_upload (image_id, user_id, slot, expires_at, delete_after, created_at)
                        VALUES (:imageId, :userId, :slot, NULL, :deleteAfter, now())
                        ON CONFLICT (image_id) DO UPDATE
                        SET expires_at = NULL, delete_after = :deleteAfter
                        """)
                .param("imageId", image.imageId().value())
                .param("userId", image.userId().value())
                .param("slot", image.slot().name())
                .param("deleteAfter", at(deleteAfter))
                .update();
    }

    @Override
    public void discardAllPending(UserId userId) {
        jdbc.sql("UPDATE pending_upload SET expires_at = NULL WHERE user_id = :userId AND expires_at IS NOT NULL")
                .param("userId", userId.value())
                .update();
    }

    @Override
    public void discardPending(UserId userId, ImageSlot slot) {
        jdbc.sql("UPDATE pending_upload SET expires_at = NULL WHERE user_id = :userId AND slot = :slot AND expires_at IS NOT NULL")
                .param("userId", userId.value())
                .param("slot", slot.name())
                .update();
    }

    @Override
    public void delete(ImageId imageId) {
        jdbc.sql("DELETE FROM pending_upload WHERE image_id = :imageId")
                .param("imageId", imageId.value())
                .update();
    }

    @Override
    public Optional<PendingUpload> lockNextDue(Instant now) {
        return jdbc.sql("SELECT " + COLUMNS + """
                         FROM pending_upload
                        WHERE delete_after <= :now
                        ORDER BY delete_after
                        LIMIT 1
                        FOR UPDATE SKIP LOCKED
                        """)
                .param("now", at(now))
                .query(JdbcPendingUploadRepository::map)
                .optional();
    }

    @Override
    public void postpone(ImageId imageId, Instant deleteAfter) {
        jdbc.sql("UPDATE pending_upload SET delete_after = :deleteAfter WHERE image_id = :imageId")
                .param("deleteAfter", at(deleteAfter))
                .param("imageId", imageId.value())
                .update();
    }

    private static PendingUpload map(ResultSet rs, int rowNum) throws SQLException {
        OwnedImage image = new OwnedImage(
                UserId.of(rs.getLong("user_id")),
                ImageSlot.valueOf(rs.getString("slot")),
                new ImageId(rs.getObject("image_id", UUID.class)));
        OffsetDateTime expiresAt = rs.getObject("expires_at", OffsetDateTime.class);
        return new PendingUpload(
                image,
                expiresAt == null ? null : expiresAt.toInstant(),
                rs.getObject("delete_after", OffsetDateTime.class).toInstant());
    }
}
