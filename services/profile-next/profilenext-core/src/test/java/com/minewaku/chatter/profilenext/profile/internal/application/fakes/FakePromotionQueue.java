package com.minewaku.chatter.profilenext.profile.internal.application.fakes;

import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.PendingImagePromotionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** An in-memory promotion queue that follows the rules of the port (lease, reported rows, ignore duplicates). */
public class FakePromotionQueue implements PendingImagePromotionRepository {

    public static final class Row {
        public final OwnedImage image;
        public final Instant createdAt;
        public int attempts;
        public Instant nextAttemptAt;
        public Instant reportedAt;

        Row(OwnedImage image, Instant now) {
            this.image = image;
            this.createdAt = now;
            this.nextAttemptAt = now;
        }
    }

    public final Map<ImageId, Row> rows = new LinkedHashMap<>();

    @Override
    public void enqueue(OwnedImage image, Instant now) {
        rows.putIfAbsent(image.imageId(), new Row(image, now));
    }

    @Override
    public List<PendingPromotion> takeDue(Instant now, int limit, Duration lease) {
        List<Row> due = rows.values().stream()
                .filter(row -> row.reportedAt == null && !row.nextAttemptAt.isAfter(now))
                .sorted(Comparator.comparing(row -> row.nextAttemptAt))
                .limit(limit)
                .toList();
        due.forEach(row -> row.nextAttemptAt = now.plus(lease));
        return due.stream()
                .map(row -> new PendingPromotion(row.image, row.attempts, row.createdAt))
                .toList();
    }

    @Override
    public void markPromoted(ImageId imageId) {
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
    public void remove(ImageId imageId) {
        rows.remove(imageId);
    }

    @Override
    public void deleteByUserId(UserId userId) {
        rows.values().removeIf(row -> row.image.userId().equals(userId));
    }
}
