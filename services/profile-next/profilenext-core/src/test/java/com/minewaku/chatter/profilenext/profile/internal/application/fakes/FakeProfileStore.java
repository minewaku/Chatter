package com.minewaku.chatter.profilenext.profile.internal.application.fakes;

import com.minewaku.chatter.profilenext.profile.internal.model.Bio;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.Profile;
import com.minewaku.chatter.profilenext.profile.internal.model.StoredImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.query.ProfileQueryRepository;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.ProfileRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** An in-memory stand-in for both profile ports, with the same rules as the SQL adapters. */
public class FakeProfileStore implements ProfileRepository, ProfileQueryRepository {

    public final Map<Long, Profile> rows = new HashMap<>();

    @Override
    public boolean applyEdit(UserId userId, DisplayName displayName, Bio bio, Instant now) {
        Profile existing = rows.get(userId.value());
        String name = displayName != null ? displayName.value() : existing == null ? "" : existing.displayName();
        String text = bio != null ? bio.value() : existing == null ? "" : existing.bio();
        if (existing != null && existing.displayName().equals(name) && existing.bio().equals(text)) {
            return false;
        }
        rows.put(
                userId.value(),
                new Profile(
                        userId,
                        name,
                        text,
                        existing == null ? null : existing.avatar(),
                        existing == null ? null : existing.banner(),
                        existing == null ? now : existing.createdAt(),
                        now));
        return true;
    }

    @Override
    public Replacement replaceImage(UserId userId, ImageSlot slot, StoredImage image, Instant now) {
        Profile existing = rows.getOrDefault(userId.value(), emptyRow(userId, now));
        StoredImage current = slot == ImageSlot.AVATAR ? existing.avatar() : existing.banner();
        if (current != null && image.uploadId().compareTo(current.uploadId()) <= 0) {
            rows.putIfAbsent(userId.value(), existing);
            return new Replacement(false, current);
        }
        rows.put(
                userId.value(),
                new Profile(
                        userId,
                        existing.displayName(),
                        existing.bio(),
                        slot == ImageSlot.AVATAR ? image : existing.avatar(),
                        slot == ImageSlot.BANNER ? image : existing.banner(),
                        existing.createdAt(),
                        now));
        return new Replacement(true, current);
    }

    private static Profile emptyRow(UserId userId, Instant now) {
        return new Profile(userId, "", "", null, null, now, now);
    }

    @Override
    public Optional<DeletedProfile> deleteByUserId(UserId userId) {
        Profile removed = rows.remove(userId.value());
        return removed == null ? Optional.empty() : Optional.of(new DeletedProfile(removed.avatar(), removed.banner()));
    }

    @Override
    public Optional<Profile> findByUserId(UserId userId) {
        return Optional.ofNullable(rows.get(userId.value()));
    }

    @Override
    public Set<String> findReferencedPublicIds(Collection<String> publicIds) {
        Set<String> referenced = new HashSet<>();
        for (Profile profile : rows.values()) {
            for (StoredImage image : new StoredImage[] {profile.avatar(), profile.banner()}) {
                if (image != null && publicIds.contains(image.publicId())) {
                    referenced.add(image.publicId());
                }
            }
        }
        return referenced;
    }
}
