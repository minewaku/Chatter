package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.profile.write.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.infrastructure.support.JdbcTestConfiguration;
import com.minewaku.chatter.profilenext.infrastructure.support.PostgreSqlTestSupport;
import com.minewaku.chatter.profilenext.profile.internal.model.Bio;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.ProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

    private static final ImageId OLD_ID = new ImageId(UUID.fromString("0197aaaa-0000-7000-8000-000000000001"));
    private static final ImageId NEW_ID = new ImageId(UUID.fromString("0197aaab-0000-7000-8000-000000000001"));

    @Test
    void replacingAnImageOnAMissingRowCreatesTheRowWithEmptyText() {
        UserId user = freshUser();

        Optional<ImageId> previous = profiles.replaceImage(user, ImageSlot.AVATAR, OLD_ID, T1);

        assertThat(previous).isEmpty();
        Map<String, Object> row = row(user);
        assertThat(row.get("avatar_image_id")).isEqualTo(OLD_ID.value());
        assertThat(row.get("banner_image_id")).isNull();
        assertThat(row.get("display_name")).isEqualTo("");
        assertThat(((java.sql.Timestamp) row.get("created_at")).toInstant()).isEqualTo(T1);
    }

    @Test
    void replacingReturnsThePreviousImageAndMovesUpdatedAt() {
        UserId user = freshUser();
        profiles.replaceImage(user, ImageSlot.AVATAR, OLD_ID, T1);

        Optional<ImageId> previous = profiles.replaceImage(user, ImageSlot.AVATAR, NEW_ID, T2);

        assertThat(previous).contains(OLD_ID);
        assertThat(row(user).get("avatar_image_id")).isEqualTo(NEW_ID.value());
        assertThat(updatedAt(user)).isEqualTo(T2);
    }

    @Test
    void theOtherSlotIsUntouchedByAReplacement() {
        UserId user = freshUser();
        profiles.replaceImage(user, ImageSlot.BANNER, OLD_ID, T1);

        profiles.replaceImage(user, ImageSlot.AVATAR, NEW_ID, T2);

        assertThat(row(user).get("banner_image_id")).isEqualTo(OLD_ID.value());
        assertThat(row(user).get("avatar_image_id")).isEqualTo(NEW_ID.value());
    }

    @Test
    void anImageReplacementKeepsTheSavedText() {
        UserId user = freshUser();
        profiles.applyEdit(user, DisplayName.of("Mine"), Bio.of("Hello"), T1);

        profiles.replaceImage(user, ImageSlot.AVATAR, OLD_ID, T2);

        assertThat(row(user).get("display_name")).isEqualTo("Mine");
        assertThat(row(user).get("bio")).isEqualTo("Hello");
    }

    @Test
    void aTextSaveKeepsBothImages() {
        UserId user = freshUser();
        profiles.replaceImage(user, ImageSlot.AVATAR, OLD_ID, T1);
        profiles.replaceImage(user, ImageSlot.BANNER, NEW_ID, T1);

        profiles.applyEdit(user, DisplayName.of("Mine"), null, T2);

        assertThat(profiles.getOrDefault(user).imageIn(ImageSlot.AVATAR)).isEqualTo(OLD_ID);
        assertThat(profiles.getOrDefault(user).imageIn(ImageSlot.BANNER)).isEqualTo(NEW_ID);
    }

    @Test
    void findHasNoImageForAnEmptySlotAndForNoRow() {
        UserId user = freshUser();
        assertThat(profiles.getOrDefault(user).imageIn(ImageSlot.AVATAR)).isNull();

        profiles.applyEdit(user, DisplayName.of("Mine"), null, T1);

        assertThat(profiles.getOrDefault(user).imageIn(ImageSlot.AVATAR)).isNull();
    }

    @Test
    void removingEmptiesTheSlotAndReturnsWhatItHeld() {
        UserId user = freshUser();
        profiles.replaceImage(user, ImageSlot.AVATAR, OLD_ID, T1);

        Optional<ImageId> removed = profiles.removeImage(user, ImageSlot.AVATAR, T2);

        assertThat(removed).contains(OLD_ID);
        assertThat(row(user).get("avatar_image_id")).isNull();
        assertThat(updatedAt(user)).isEqualTo(T2);
    }

    @Test
    void removingAnEmptySlotOrAMissingRowChangesNothing() {
        UserId user = freshUser();
        assertThat(profiles.removeImage(user, ImageSlot.AVATAR, T1)).isEmpty();
        assertThat(rowCount(user)).isZero();

        profiles.applyEdit(user, DisplayName.of("Mine"), null, T1);
        assertThat(profiles.removeImage(user, ImageSlot.AVATAR, T2)).isEmpty();
        assertThat(updatedAt(user)).isEqualTo(T1);
    }

    @Test
    void twoConcurrentReplacementsEachSeeTheOtherOrTheOriginalAndNeverTheSamePrevious() throws Exception {
        UserId user = freshUser();
        profiles.replaceImage(user, ImageSlot.AVATAR, OLD_ID, T1);
        ImageId a = new ImageId(UUID.fromString("0197aaac-0000-7000-8000-000000000001"));
        ImageId b = new ImageId(UUID.fromString("0197aaad-0000-7000-8000-000000000001"));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch go = new CountDownLatch(1);
            Future<Optional<ImageId>> first = pool.submit(() -> {
                go.await();
                return profiles.replaceImage(user, ImageSlot.AVATAR, a, T2);
            });
            Future<Optional<ImageId>> second = pool.submit(() -> {
                go.await();
                return profiles.replaceImage(user, ImageSlot.AVATAR, b, T3);
            });
            go.countDown();

            List<ImageId> previous = List.of(first.get().orElseThrow(), second.get().orElseThrow());

            // a chain, not a fork: exactly one saw the original, the other saw the winner of the first
            assertThat(previous).contains(OLD_ID);
            assertThat(previous).doesNotHaveDuplicates();
            assertThat(profiles.getOrDefault(user).imageIn(ImageSlot.AVATAR)).isIn(a, b);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void deleteByUserIdReturnsBothImagesAndRemovesTheRow() {
        UserId user = freshUser();
        profiles.replaceImage(user, ImageSlot.AVATAR, NEW_ID, T1);
        profiles.replaceImage(user, ImageSlot.BANNER, OLD_ID, T1);

        Optional<ProfileRepository.DeletedProfile> deleted = profiles.deleteByUserId(user);

        assertThat(deleted).isPresent();
        assertThat(deleted.get().avatar()).isEqualTo(NEW_ID);
        assertThat(deleted.get().banner()).isEqualTo(OLD_ID);
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
