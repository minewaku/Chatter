package com.minewaku.chatter.profilenext.profile.internal.application.fakes;

import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
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

/** An in-memory removal queue that follows the port's rules (lease, reported rows, ignore duplicates). */
public class FakeRemovalQueue implements PendingImageRemovalRepository {

    public static final class Row {
        public final OwnedImage image;
        public int attempts;
        public Instant nextAttemptAt;
        public Instant reportedAt;

        Row(OwnedImage image, Instant nextAttemptAt) {
            this.image = image;
            this.nextAttemptAt = nextAttemptAt;
        }
    }

    public final Map<ImageId, Row> rows = new LinkedHashMap<>();

    @Override
    public void enqueue(OwnedImage image, Instant now) {
        rows.putIfAbsent(image.imageId(), new Row(image, now));
    }

    @Override
    public List<PendingRemoval> takeDue(Instant now, int limit, Duration lease) {
        List<Row> due = rows.values().stream()
                .filter(row -> row.reportedAt == null && !row.nextAttemptAt.isAfter(now))
                .sorted(Comparator.comparing(row -> row.nextAttemptAt))
                .limit(limit)
                .toList();
        due.forEach(row -> row.nextAttemptAt = now.plus(lease));
        return due.stream().map(row -> new PendingRemoval(row.image, row.attempts)).toList();
    }

    @Override
    public void markRemoved(ImageId imageId) {
        rows.remove(imageId);
    }

    @Override
    public void markFailed(ImageId imageId, int attempts, Instant nextAttemptAt, Instant reportedAt) {
        Row row = rows.get(imageId);
        row.attempts = attempts;
        row.nextAttemptAt = nextAttemptAt;
        row.reportedAt = reportedAt;
    }

    @Override
    public Set<ImageId> findActive(Collection<ImageId> imageIds) {
        return rows.values().stream()
                .filter(row -> row.reportedAt == null && imageIds.contains(row.image.imageId()))
                .map(row -> row.image.imageId())
                .collect(Collectors.toSet());
    }

    @Override
    public void clear(ImageId imageId) {
        rows.remove(imageId);
    }
}
