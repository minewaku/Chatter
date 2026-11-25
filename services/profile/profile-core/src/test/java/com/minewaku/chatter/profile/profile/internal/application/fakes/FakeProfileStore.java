package com.minewaku.chatter.profile.profile.internal.application.fakes;

import com.minewaku.chatter.profile.profile.internal.model.Bio;
import com.minewaku.chatter.profile.profile.internal.model.DisplayName;
import com.minewaku.chatter.profile.profile.internal.model.ImageId;
import com.minewaku.chatter.profile.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profile.profile.internal.model.Profile;
import com.minewaku.chatter.profile.profile.internal.model.UserId;
import com.minewaku.chatter.profile.profile.internal.port.query.ProfileQueryRepository;
import com.minewaku.chatter.profile.profile.internal.port.repository.ProfileRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** An in-memory stand-in for both profile ports, with the same rules as the SQL adapters. */
public class FakeProfileStore implements ProfileRepository, ProfileQueryRepository {

    public final Map<Long, Profile> rows = new HashMap<>();

    @Override
    public boolean save(UserId userId, DisplayName displayName, Bio bio, Instant now) {
        Profile existing = rows.get(userId.value());
        DisplayName name = displayName != null ? displayName : existing == null ? null : existing.displayName();
        Bio text = bio != null ? bio : existing == null ? Bio.of("") : existing.bio();
        if (existing != null && Objects.equals(existing.displayName(), name) && existing.bio().equals(text)) {
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
    public Profile getOrDefault(UserId userId) {
        return rows.getOrDefault(userId.value(), Profile.defaultsFor(userId));
    }

    @Override
    public Optional<ImageId> replaceImage(UserId userId, ImageSlot slot, ImageId imageId, Instant now) {
        Profile existing = rows.get(userId.value());
        if (existing == null) {
            existing = new Profile(userId, null, Bio.of(""), null, null, now, now);
        }
        ImageId previous = existing.imageIn(slot);
        rows.put(userId.value(), withImage(existing, slot, imageId, now));
        return Optional.ofNullable(previous);
    }

    @Override
    public Optional<ImageId> clearImage(UserId userId, ImageSlot slot, Instant now) {
        Profile existing = rows.get(userId.value());
        if (existing == null || existing.imageIn(slot) == null) {
            return Optional.empty();
        }
        ImageId previous = existing.imageIn(slot);
        rows.put(userId.value(), withImage(existing, slot, null, now));
        return Optional.of(previous);
    }

    private static Profile withImage(Profile profile, ImageSlot slot, ImageId imageId, Instant now) {
        return new Profile(
                profile.userId(),
                profile.displayName(),
                profile.bio(),
                slot == ImageSlot.AVATAR ? imageId : profile.avatar(),
                slot == ImageSlot.BANNER ? imageId : profile.banner(),
                profile.createdAt(),
                now);
    }

    @Override
    public Optional<DeletedProfile> deleteByUserId(UserId userId) {
        Profile removed = rows.remove(userId.value());
        return removed == null ? Optional.empty() : Optional.of(new DeletedProfile(removed.avatar(), removed.banner()));
    }

    @Override
    public Optional<ProfileView> findByUserId(UserId userId) {
        Profile profile = rows.get(userId.value());
        return profile == null
                ? Optional.empty()
                : Optional.of(new ProfileView(
                        profile.userId().value(),
                        profile.displayName() == null ? "" : profile.displayName().value(),
                        profile.bio().value(),
                        profile.avatar() == null ? null : profile.avatar().value(),
                        profile.banner() == null ? null : profile.banner().value(),
                        profile.createdAt(),
                        profile.updatedAt()));
    }
}
