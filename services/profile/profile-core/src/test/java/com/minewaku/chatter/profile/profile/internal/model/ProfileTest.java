package com.minewaku.chatter.profile.profile.internal.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profile.profile.internal.application.fakes.ImageIds;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProfileTest {

    @Test
    void aProfileOfAUserWhoNeverSavedHasNoImagesAndNoTimestamps() {
        Profile profile = Profile.defaultsFor(UserId.of(7));

        assertThat(profile.displayName()).isNull();
        assertThat(profile.bio()).isEqualTo(Bio.of(""));
        assertThat(profile.avatar()).isNull();
        assertThat(profile.banner()).isNull();
        assertThat(profile.createdAt()).isNull();
        assertThat(profile.updatedAt()).isNull();
    }

    @Test
    void imageInReturnsTheIdOfTheAskedSlot() {
        Instant now = Instant.parse("2026-11-04T10:00:00Z");
        Profile profile =
                new Profile(UserId.of(7), null, Bio.of(""), ImageIds.at(1), ImageIds.at(2), now, now);

        assertThat(profile.imageIn(ImageSlot.AVATAR)).isEqualTo(ImageIds.at(1));
        assertThat(profile.imageIn(ImageSlot.BANNER)).isEqualTo(ImageIds.at(2));
        assertThat(Profile.defaultsFor(UserId.of(7)).imageIn(ImageSlot.AVATAR)).isNull();
    }

    @Test
    void aProfileStoresNothingAboutAnImageButItsIdentity() {
        Set<Class<?>> allowed = Set.of(UserId.class, DisplayName.class, Bio.class, ImageId.class, Instant.class);

        assertThat(Arrays.stream(Profile.class.getRecordComponents()).map(RecordComponent::getType))
                .allMatch(allowed::contains);
    }
}
