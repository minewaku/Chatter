package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.profile.write.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.infrastructure.support.JdbcTestConfiguration;
import com.minewaku.chatter.profilenext.infrastructure.support.PostgreSqlTestSupport;
import com.minewaku.chatter.profilenext.profile.internal.model.Bio;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.StoredImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.ProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(JdbcTestConfiguration.class)
class JdbcProfileRepositoryTest extends PostgreSqlTestSupport {

    private static final AtomicLong IDS = new AtomicLong(8_000_000L);
    private static final Instant T1 = Instant.parse("2026-11-04T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-11-04T10:05:00Z");
    private static final Instant T3 = Instant.parse("2026-11-04T10:10:00Z");

    @Autowired
    private ProfileRepository profiles;

    @Autowired
    private JdbcClient jdbc;

    private static UserId freshUser() {
        return UserId.of(IDS.incrementAndGet());
    }

    private Map<String, Object> row(UserId userId) {
        return jdbc.sql("SELECT * FROM profile WHERE user_id = :id")
                .param("id", userId.value())
                .query()
                .singleRow();
    }

    private int rowCount(UserId userId) {
        return jdbc.sql("SELECT count(*) FROM profile WHERE user_id = :id")
                .param("id", userId.value())
                .query(Integer.class)
                .single();
    }

    private Instant updatedAt(UserId userId) {
        return ((java.sql.Timestamp) row(userId).get("updated_at")).toInstant();
    }

    @Test
    void theFirstSaveInsertsARowWithBothTimestampsAtNow() {
        UserId user = freshUser();

        boolean written = profiles.applyEdit(user, DisplayName.of("Mine"), null, T1);

        assertThat(written).isTrue();
        Map<String, Object> row = row(user);
        assertThat(row.get("display_name")).isEqualTo("Mine");
        assertThat(row.get("bio")).isEqualTo("");
        assertThat(((java.sql.Timestamp) row.get("created_at")).toInstant()).isEqualTo(T1);
        assertThat(updatedAt(user)).isEqualTo(T1);
    }

    @Test
    void aBioOnlySaveLeavesTheDisplayNameUntouched() {
        UserId user = freshUser();
        profiles.applyEdit(user, DisplayName.of("Mine"), null, T1);

        profiles.applyEdit(user, null, Bio.of("Hello"), T2);

        assertThat(row(user).get("display_name")).isEqualTo("Mine");
        assertThat(row(user).get("bio")).isEqualTo("Hello");
    }

    @Test
    void aBioOnlyFirstSaveCreatesTheRowWithAnEmptyName() {
        UserId user = freshUser();

        profiles.applyEdit(user, null, Bio.of("Hello"), T1);

        assertThat(row(user).get("display_name")).isEqualTo("");
        assertThat(row(user).get("bio")).isEqualTo("Hello");
    }

    @Test
    void aSaveEqualToTheStoredValuesChangesNothingAndKeepsUpdatedAt() {
        UserId user = freshUser();
        profiles.applyEdit(user, DisplayName.of("Mine"), Bio.of("Hello"), T1);

        boolean written = profiles.applyEdit(user, DisplayName.of("Mine"), Bio.of("Hello"), T2);

        assertThat(written).isFalse();
        assertThat(updatedAt(user)).isEqualTo(T1);
    }

    @Test
    void updatedAtChangesOnEveryRealChange() {
        UserId user = freshUser();
        profiles.applyEdit(user, DisplayName.of("Mine"), Bio.of("Hello"), T1);

        assertThat(profiles.applyEdit(user, DisplayName.of("Other"), null, T2)).isTrue();
        assertThat(updatedAt(user)).isEqualTo(T2);

        assertThat(profiles.applyEdit(user, null, Bio.of("Changed"), T3)).isTrue();
        assertThat(updatedAt(user)).isEqualTo(T3);
    }

    @Test
    void sendingAnEmptyBioClearsIt() {
        UserId user = freshUser();
        profiles.applyEdit(user, DisplayName.of("Mine"), Bio.of("Hello"), T1);

        profiles.applyEdit(user, null, Bio.of(""), T2);

        assertThat(row(user).get("bio")).isEqualTo("");
        assertThat(row(user).get("display_name")).isEqualTo("Mine");
    }

    @Test
    void twoConcurrentFirstSavesLeaveOneRowAndNeverMixTheirValues() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 50; round++) {
                UserId user = freshUser();
                CountDownLatch go = new CountDownLatch(1);
                Future<Boolean> first = pool.submit(() -> {
                    go.await();
                    return profiles.applyEdit(user, DisplayName.of("Alpha"), Bio.of("bio from alpha"), T1);
                });
                Future<Boolean> second = pool.submit(() -> {
                    go.await();
                    return profiles.applyEdit(user, DisplayName.of("Beta"), Bio.of("bio from beta"), T2);
                });
                go.countDown();
                first.get();
                second.get();

                assertThat(rowCount(user)).isEqualTo(1);
                Map<String, Object> row = row(user);
                List<String> pair = List.of((String) row.get("display_name"), (String) row.get("bio"));
                assertThat(pair)
                        .isIn(List.of("Alpha", "bio from alpha"), List.of("Beta", "bio from beta"));
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static final String OLD_ID = "0197aaaa-0000-7000-8000-000000000001";
    private static final String NEW_ID = "0197aaab-0000-7000-8000-000000000001";

    private static StoredImage avatar(UserId user, String uploadId) {
        return new StoredImage(
                "chatter/profile/avatars/" + user.value() + "/" + uploadId, "https://img/" + uploadId);
    }

    @Test
    void replacingAnImageOnAMissingRowCreatesTheRowWithEmptyText() {
        UserId user = freshUser();

        var replacement = profiles.replaceImage(user, ImageSlot.AVATAR, avatar(user, OLD_ID), T1);

        assertThat(replacement.applied()).isTrue();
        assertThat(replacement.previous()).isNull();
        Map<String, Object> row = row(user);
        assertThat(row.get("avatar_public_id")).isEqualTo(avatar(user, OLD_ID).publicId());
        assertThat(row.get("avatar_url")).isEqualTo("https://img/" + OLD_ID);
        assertThat(row.get("display_name")).isEqualTo("");
        assertThat(row.get("banner_public_id")).isNull();
    }

    @Test
    void aNewerUploadIdReplacesTheSlotAndReturnsThePreviousImage() {
        UserId user = freshUser();
        profiles.replaceImage(user, ImageSlot.AVATAR, avatar(user, OLD_ID), T1);

        var replacement = profiles.replaceImage(user, ImageSlot.AVATAR, avatar(user, NEW_ID), T2);

        assertThat(replacement.applied()).isTrue();
        assertThat(replacement.previous()).isEqualTo(avatar(user, OLD_ID));
        assertThat(row(user).get("avatar_public_id")).isEqualTo(avatar(user, NEW_ID).publicId());
        assertThat(updatedAt(user)).isEqualTo(T2);
    }

    @Test
    void theSameOrAnOlderUploadIdChangesNothingAndReportsTheStoredImage() {
        UserId user = freshUser();
        profiles.replaceImage(user, ImageSlot.AVATAR, avatar(user, NEW_ID), T1);

        var same = profiles.replaceImage(user, ImageSlot.AVATAR, avatar(user, NEW_ID), T2);
        var older = profiles.replaceImage(user, ImageSlot.AVATAR, avatar(user, OLD_ID), T3);

        assertThat(same.applied()).isFalse();
        assertThat(same.previous()).isEqualTo(avatar(user, NEW_ID));
        assertThat(older.applied()).isFalse();
        assertThat(older.previous()).isEqualTo(avatar(user, NEW_ID));
        assertThat(row(user).get("avatar_public_id")).isEqualTo(avatar(user, NEW_ID).publicId());
        assertThat(updatedAt(user)).isEqualTo(T1);
    }

    @Test
    void theOtherSlotIsUntouched() {
        UserId user = freshUser();
        StoredImage banner = new StoredImage(
                "chatter/profile/banners/" + user.value() + "/" + OLD_ID, "https://img/banner");
        profiles.replaceImage(user, ImageSlot.BANNER, banner, T1);

        profiles.replaceImage(user, ImageSlot.AVATAR, avatar(user, NEW_ID), T2);

        assertThat(row(user).get("banner_public_id")).isEqualTo(banner.publicId());
        assertThat(row(user).get("avatar_public_id")).isEqualTo(avatar(user, NEW_ID).publicId());
    }

    @Test
    void anImageReplacementKeepsTheSavedText() {
        UserId user = freshUser();
        profiles.applyEdit(user, DisplayName.of("Mine"), Bio.of("Hello"), T1);

        profiles.replaceImage(user, ImageSlot.AVATAR, avatar(user, OLD_ID), T2);

        assertThat(row(user).get("display_name")).isEqualTo("Mine");
        assertThat(row(user).get("bio")).isEqualTo("Hello");
    }

    @Test
    void deleteByUserIdReturnsBothImagesAndRemovesTheRow() {
        UserId user = freshUser();
        StoredImage banner = new StoredImage(
                "chatter/profile/banners/" + user.value() + "/" + OLD_ID, "https://img/banner");
        profiles.replaceImage(user, ImageSlot.AVATAR, avatar(user, NEW_ID), T1);
        profiles.replaceImage(user, ImageSlot.BANNER, banner, T1);

        Optional<ProfileRepository.DeletedProfile> deleted = profiles.deleteByUserId(user);

        assertThat(deleted).isPresent();
        assertThat(deleted.get().avatar()).isEqualTo(avatar(user, NEW_ID));
        assertThat(deleted.get().banner()).isEqualTo(banner);
        assertThat(rowCount(user)).isZero();
    }

    @Test
    void deleteByUserIdIsEmptyWhenThereIsNoRowAndASecondCallIsANoOp() {
        UserId user = freshUser();
        assertThat(profiles.deleteByUserId(user)).isEmpty();

        profiles.applyEdit(user, DisplayName.of("Mine"), null, T1);
        assertThat(profiles.deleteByUserId(user)).isPresent();
        assertThat(profiles.deleteByUserId(user)).isEmpty();
    }
}
