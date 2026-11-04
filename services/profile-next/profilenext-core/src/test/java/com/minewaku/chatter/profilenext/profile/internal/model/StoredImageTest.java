package com.minewaku.chatter.profilenext.profile.internal.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class StoredImageTest {

    private static StoredImage image(String uploadId) {
        return new StoredImage("chatter/profile/avatars/7/" + uploadId, "https://img/" + uploadId);
    }

    @Test
    void theUploadIdIsTheLastPathSegment() {
        assertThat(image("0197aaaa-0000-7000-8000-000000000001").uploadId())
                .isEqualTo("0197aaaa-0000-7000-8000-000000000001");
    }

    @Test
    void idsOfTheSameFormatOrderByTimeWhenComparedAsText() {
        String older = "0197aaaa-0000-7000-8000-000000000001";
        String newer = "0197aaab-0000-7000-8000-000000000001";

        assertThat(List.of(newer, older).stream().sorted()).containsExactly(older, newer);
        assertThat(image(newer).uploadId()).isGreaterThan(image(older).uploadId());
    }

    @Test
    void aConfirmedUploadKnowsWhetherItsPublicIdBelongsToItsUserAndSlot() {
        String id = "chatter/profile/avatars/7/abc";
        assertThat(new ConfirmedUpload(UserId.of(7), ImageSlot.AVATAR, id, "u", "png", 1).publicIdMatchesOwner())
                .isTrue();
        assertThat(new ConfirmedUpload(UserId.of(8), ImageSlot.AVATAR, id, "u", "png", 1).publicIdMatchesOwner())
                .isFalse();
        assertThat(new ConfirmedUpload(UserId.of(7), ImageSlot.BANNER, id, "u", "png", 1).publicIdMatchesOwner())
                .isFalse();
        assertThat(new ConfirmedUpload(UserId.of(7), ImageSlot.AVATAR, "elsewhere/7/abc", "u", "png", 1)
                        .publicIdMatchesOwner())
                .isFalse();
    }
}
