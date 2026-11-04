package com.minewaku.chatter.profilenext.profile.internal.application.fakes;

import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.PendingRemoval;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingImageRemovalRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** An in-memory removal queue that follows the SQL adapter's rules (lease, reported rows, ignore duplicates). */
public class FakeRemovalQueue implements PendingImageRemovalRepository {

    public static final class Row {
        public final String publicId;
        public final ImageSlot slot;
        public int attempts;
        public Instant nextAttemptAt;
        public Instant reportedAt;

        Row(String publicId, ImageSlot slot, Instant nextAttemptAt) {
            this.publicId = publicId;
            this.slot = slot;
            this.nextAttemptAt = nextAttemptAt;
        }
    }

    public final Map<String, Row> rows = new LinkedHashMap<>();

    @Override
    public void enqueue(String publicId, ImageSlot slot, Instant now) {
        rows.putIfAbsent(publicId, new Row(publicId, slot, now));
    }

    @Override
    public List<PendingRemoval> takeDue(Instant now, int limit, Duration lease) {
        List<Row> due = rows.values().stream()
                .filter(row -> row.reportedAt == null && !row.nextAttemptAt.isAfter(now))
                .sorted(Comparator.comparing(row -> row.nextAttemptAt))
                .limit(limit)
                .toList();
        due.forEach(row -> row.nextAttemptAt = now.plus(lease));
        return due.stream().map(row -> new PendingRemoval(row.publicId, row.slot, row.attempts)).toList();
    }

    @Override
    public void markRemoved(String publicId) {
        rows.remove(publicId);
    }

    @Override
    public void markFailed(String publicId, int attempts, Instant nextAttemptAt, Instant reportedAt) {
        Row row = rows.get(publicId);
        row.attempts = attempts;
        row.nextAttemptAt = nextAttemptAt;
        row.reportedAt = reportedAt;
    }

    @Override
    public Set<String> findActivePublicIds(Collection<String> publicIds) {
        return rows.values().stream()
                .filter(row -> row.reportedAt == null && publicIds.contains(row.publicId))
                .map(row -> row.publicId)
                .collect(Collectors.toSet());
    }

    @Override
    public void clear(String publicId) {
        rows.remove(publicId);
    }
}
