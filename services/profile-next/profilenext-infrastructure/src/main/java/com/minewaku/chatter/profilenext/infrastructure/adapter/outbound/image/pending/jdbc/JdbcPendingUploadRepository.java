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
 * The upload ledger in PostgreSQL. A partial unique index allows one pending ticket per user and slot, which is what
 * turns two racing ticket requests into one winner; {@code lockNextDue} skips rows another worker holds, so sweepers on
 * several instances never share a row.
 */
@Component
public class JdbcPendingUploadRepository implements PendingUploadRepository {

    private static final String COLUMNS = "image_id, user_id, slot, state, expires_at, delete_after";

    private final JdbcClient jdbc;

    JdbcPendingUploadRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static OffsetDateTime at(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    @Override
    public Optional<PendingUpload> findPending(UserId userId, ImageSlot slot) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM pending_upload WHERE user_id = :userId AND slot = :slot AND state = 'PENDING'")
                .param("userId", userId.value())
                .param("slot", slot.name())
                .query(JdbcPendingUploadRepository::map)
                .optional();
    }

    @Override
    public Optional<PendingUpload> findPending(UserId userId, ImageId imageId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM pending_upload WHERE image_id = :imageId AND user_id = :userId AND state = 'PENDING'")
                .param("imageId", imageId.value())
                .param("userId", userId.value())
                .query(JdbcPendingUploadRepository::map)
                .optional();
    }

    @Override
    public Optional<PendingUpload> lockPending(UserId userId, ImageId imageId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM pending_upload WHERE image_id = :imageId AND user_id = :userId AND state = 'PENDING' FOR UPDATE")
                .param("imageId", imageId.value())
                .param("userId", userId.value())
                .query(JdbcPendingUploadRepository::map)
                .optional();
    }

    @Override
    public boolean insertPending(PendingUpload upload) {
        int inserted = jdbc.sql("""
                        INSERT INTO pending_upload (image_id, user_id, slot, state, expires_at, delete_after, created_at)
                        VALUES (:imageId, :userId, :slot, 'PENDING', :expiresAt, :deleteAfter, now())
                        ON CONFLICT (user_id, slot) WHERE state = 'PENDING' DO NOTHING
                        """)
                .param("imageId", upload.image().imageId().value())
                .param("userId", upload.image().userId().value())
                .param("slot", upload.image().slot().name())
                .param("expiresAt", at(upload.expiresAt()))
                .param("deleteAfter", at(upload.deleteAfter()))
                .update();
        return inserted > 0;
    }

    @Override
    public void discard(OwnedImage image, Instant deleteAfter) {
        jdbc.sql("""
                        INSERT INTO pending_upload (image_id, user_id, slot, state, expires_at, delete_after, created_at)
                        VALUES (:imageId, :userId, :slot, 'DISCARD', NULL, :deleteAfter, now())
                        ON CONFLICT (image_id) DO UPDATE
                        SET state = 'DISCARD', expires_at = NULL, delete_after = :deleteAfter
                        """)
                .param("imageId", image.imageId().value())
                .param("userId", image.userId().value())
                .param("slot", image.slot().name())
                .param("deleteAfter", at(deleteAfter))
                .update();
    }

    @Override
    public void discardAllPending(UserId userId) {
        jdbc.sql("UPDATE pending_upload SET state = 'DISCARD', expires_at = NULL WHERE user_id = :userId AND state = 'PENDING'")
                .param("userId", userId.value())
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
                PendingUpload.State.valueOf(rs.getString("state")),
                expiresAt == null ? null : expiresAt.toInstant(),
                rs.getObject("delete_after", OffsetDateTime.class).toInstant());
    }
}
