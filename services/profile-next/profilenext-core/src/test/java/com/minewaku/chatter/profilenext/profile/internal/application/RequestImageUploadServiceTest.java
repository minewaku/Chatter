package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.profilenext.profile.api.command.RequestImageUploadUseCase;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageRateLimitedException;
import com.minewaku.chatter.profilenext.profile.api.exception.ImageStorageUnavailableException;
import com.minewaku.chatter.profilenext.profile.internal.application.command.RequestImageUploadService;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.ImageIds;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RequestImageUploadServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    private final FakeImageStorage storage = new FakeImageStorage();
    private final RequestImageUploadService service =
            new RequestImageUploadService(storage, () -> ImageIds.at(1).value().toString(), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void asksStorageForTheCallersOwnAvatarLocation() {
        var result = service.handle(new RequestImageUploadUseCase.Command(7, "AVATAR"));

        assertThat(storage.permits).hasSize(1);
        var permit = storage.permits.get(0);
        assertThat(permit.image()).isEqualTo(new OwnedImage(UserId.of(7), ImageSlot.AVATAR, ImageIds.at(1)));
        assertThat(permit.now()).isEqualTo(NOW);
        assertThat(result.fields()).containsEntry("user_id", "7").containsEntry("slot", "AVATAR");
        assertThat(result.method()).isEqualTo("POST");
        assertThat(result.expiresAt()).isAfter(NOW);
    }

    @Test
    void aBannerRequestAsksForTheBannerSlot() {
        var result = service.handle(new RequestImageUploadUseCase.Command(7, "BANNER"));

        assertThat(storage.permits.get(0).image().slot()).isEqualTo(ImageSlot.BANNER);
        assertThat(result.fields()).containsEntry("slot", "BANNER");
    }

    @Test
    void twoRequestsForTheSameSlotUseDifferentIdsAndTheSecondIsNewer() {
        AtomicInteger counter = new AtomicInteger();
        RequestImageUploadService counting = new RequestImageUploadService(
                storage, () -> ImageIds.at(counter.incrementAndGet()).value().toString(), Clock.fixed(NOW, ZoneOffset.UTC));

        counting.handle(new RequestImageUploadUseCase.Command(7, "AVATAR"));
        counting.handle(new RequestImageUploadUseCase.Command(7, "AVATAR"));

        var first = storage.permits.get(0).image().imageId();
        var second = storage.permits.get(1).image().imageId();
        assertThat(second).isNotEqualTo(first);
        assertThat(second.isNewerThan(first)).isTrue();
    }

    @Test
    void theServiceHasNoRepositoryToWriteTo() {
        var constructor = RequestImageUploadService.class.getConstructors()[0];

        assertThat(constructor.getParameterTypes())
                .noneMatch(type -> type.getPackageName().contains(".port.repository")
                        || type.getPackageName().contains(".port.query"));
    }

    @Test
    void usesExactlyTheUserOfTheCommand() {
        service.handle(new RequestImageUploadUseCase.Command(7, "AVATAR"));
        service.handle(new RequestImageUploadUseCase.Command(8, "BANNER"));

        assertThat(storage.permits.get(0).image().userId()).isEqualTo(UserId.of(7));
        assertThat(storage.permits.get(1).image().userId()).isEqualTo(UserId.of(8));
    }

    @Test
    void anOutageOfTheProviderPropagatesUnchanged() {
        storage.failPermit = true;

        assertThatThrownBy(() -> service.handle(new RequestImageUploadUseCase.Command(7, "AVATAR")))
                .isInstanceOf(ImageStorageUnavailableException.class);
        assertThat(storage.permits).isEmpty();
    }

    @Test
    void aRateLimitFromTheProviderPropagatesUnchanged() {
        storage.rateLimitPermit = true;

        assertThatThrownBy(() -> service.handle(new RequestImageUploadUseCase.Command(7, "AVATAR")))
                .isInstanceOf(ImageStorageRateLimitedException.class);
    }

    @Test
    void anIdThatIsNotAUuidFailsBeforeAnyStorageCall() {
        RequestImageUploadService broken =
                new RequestImageUploadService(storage, () -> "upload-1", Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> broken.handle(new RequestImageUploadUseCase.Command(7, "AVATAR")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(storage.permits).isEmpty();
    }
}
