package com.minewaku.chatter.profilenext.profile.internal.application.fakes;

import com.minewaku.chatter.profilenext.profile.internal.model.Bio;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageRef;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.Profile;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.query.ProfileQueryRepository;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.ProfileRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * An in-memory stand-in for both profile ports, with the same rules as the SQL adapters. It cannot show real mutual
 * exclusion, so it records the order of calls in {@link #calls} for tests that check "lock first".
 */
public class FakeProfileStore implements ProfileRepository, ProfileQueryRepository {

    public final Map<Long, Profile> rows = new HashMap<>();

    /** Calls made, in order, as {@code findForUpdate:<user>}, {@code setImage:<user>:<slot>}, {@code clearImageIf:<user>:<slot>}. */
    public final List<String> calls = new ArrayList<>();

    private final FakePromotionQueue promotions;

    public FakeProfileStore(FakePromotionQueue promotions) {
        this.promotions = promotions;
    }

    public FakeProfileStore() {
        this(new FakePromotionQueue());
    }

    @Override
    public boolean applyEdit(UserId userId, DisplayName displayName, Bio bio, Instant now) {
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
    public Optional<Profile> findForUpdate(UserId userId) {
        calls.add("findForUpdate:" + userId.value());
        return Optional.ofNullable(rows.get(userId.value()));
    }

    @Override
    public void setImage(UserId userId, ImageSlot slot, ImageId imageId, Instant now) {
        calls.add("setImage:" + userId.value() + ":" + slot);
        Profile existing = rows.get(userId.value());
        if (existing == null) {
            throw new IllegalStateException("setImage called without a row for user " + userId.value());
        }
        rows.put(userId.value(), withImage(existing, slot, imageId, now));
    }

    @Override
    public boolean clearImageIf(UserId userId, ImageSlot slot, ImageId imageId, Instant now) {
        calls.add("clearImageIf:" + userId.value() + ":" + slot);
        Profile existing = rows.get(userId.value());
        if (existing == null || !imageId.equals(existing.imageIn(slot))) {
            return false;
        }
        rows.put(userId.value(), withImage(existing, slot, null, now));
        return true;
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
        if (profile == null) {
            return Optional.empty();
        }
        return Optional.of(new ProfileView(
                profile.userId(),
                profile.displayName(),
                profile.bio(),
                refOf(profile.avatar()),
                refOf(profile.banner()),
                profile.createdAt(),
                profile.updatedAt()));
    }

    private ImageRef refOf(ImageId id) {
        return id == null ? null : new ImageRef(id, !promotions.rows.containsKey(id));
    }

    @Override
    public Set<ImageId> findReferenced(Collection<OwnedImage> images) {
        Set<ImageId> referenced = new HashSet<>();
        for (OwnedImage image : images) {
            Profile owner = rows.get(image.userId().value());
            if (owner != null && image.imageId().equals(owner.imageIn(image.slot()))) {
                referenced.add(image.imageId());
            }
        }
        return referenced;
    }
}
