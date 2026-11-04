package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.removal.jdbc;

import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.PendingRemoval;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingImageRemovalRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The removal work queue in PostgreSQL. {@code takeDue} leases what it returns so workers never share a row. */
@Component
public class JdbcPendingImageRemovalRepository implements PendingImageRemovalRepository {

    private final JdbcClient jdbc;

    JdbcPendingImageRemovalRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static OffsetDateTime at(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    @Override
    public void enqueue(String publicId, ImageSlot slot, Instant now) {
        jdbc.sql("""
                        INSERT INTO pending_image_removal (public_id, slot, attempts, next_attempt_at, created_at)
                        VALUES (:publicId, :slot, 0, :now, :now)
                        ON CONFLICT (public_id) DO NOTHING
                        """)
                .param("publicId", publicId)
                .param("slot", slot.name())
                .param("now", at(now))
                .update();
    }

    @Override
    @Transactional
    public List<PendingRemoval> takeDue(Instant now, int limit, Duration lease) {
        List<PendingRemoval> due = jdbc.sql("""
                        SELECT public_id, slot, attempts
                        FROM pending_image_removal
                        WHERE next_attempt_at <= :now AND reported_at IS NULL
                        ORDER BY next_attempt_at
                        LIMIT :limit
                        FOR UPDATE SKIP LOCKED
                        """)
                .param("now", at(now))
                .param("limit", limit)
                .query((rs, rowNum) -> new PendingRemoval(
                        rs.getString("public_id"), ImageSlot.valueOf(rs.getString("slot")), rs.getInt("attempts")))
                .list();
        if (!due.isEmpty()) {
            jdbc.sql("UPDATE pending_image_removal SET next_attempt_at = :leasedUntil WHERE public_id IN (:ids)")
                    .param("leasedUntil", at(now.plus(lease)))
                    .param("ids", due.stream().map(PendingRemoval::publicId).toList())
                    .update();
        }
        return due;
    }

    @Override
    public void markRemoved(String publicId) {
        delete(publicId);
    }

    @Override
    public void markFailed(String publicId, int attempts, Instant nextAttemptAt, Instant reportedAt) {
        jdbc.sql("""
                        UPDATE pending_image_removal
                        SET attempts = :attempts, next_attempt_at = :next, reported_at = :reported
                        WHERE public_id = :publicId
                        """)
                .param("attempts", attempts)
                .param("next", at(nextAttemptAt))
                .param("reported", at(reportedAt), java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
                .param("publicId", publicId)
                .update();
    }

    @Override
    public Set<String> findActivePublicIds(Collection<String> publicIds) {
        if (publicIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.sql("""
                        SELECT public_id FROM pending_image_removal
                        WHERE public_id IN (:ids) AND reported_at IS NULL
                        """)
                .param("ids", publicIds)
                .query(String.class)
                .list());
    }

    @Override
    public void clear(String publicId) {
        delete(publicId);
    }

    private void delete(String publicId) {
        jdbc.sql("DELETE FROM pending_image_removal WHERE public_id = :publicId")
                .param("publicId", publicId)
                .update();
    }
}
