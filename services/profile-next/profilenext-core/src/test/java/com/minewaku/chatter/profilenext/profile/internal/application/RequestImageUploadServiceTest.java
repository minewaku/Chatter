package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.profile.api.command.RequestImageUploadUseCase;
import com.minewaku.chatter.profilenext.profile.internal.application.command.RequestImageUploadService;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.FakeImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RequestImageUploadServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    private final FakeImageStorage storage = new FakeImageStorage();
    private final RequestImageUploadService service =
            new RequestImageUploadService(storage, () -> "upload-1", Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void asksStorageForTheCallersOwnAvatarLocation() {
        var result = service.handle(new RequestImageUploadUseCase.Command(7, "AVATAR"));

        assertThat(storage.permits).hasSize(1);
        var permit = storage.permits.get(0);
        assertThat(permit.userId()).isEqualTo(UserId.of(7));
        assertThat(permit.slot()).isEqualTo(ImageSlot.AVATAR);
        assertThat(permit.uploadId()).isEqualTo("upload-1");
        assertThat(permit.now()).isEqualTo(NOW);
        assertThat(result.fields()).containsEntry("public_id", "chatter/profile/avatars/7/upload-1");
        assertThat(result.method()).isEqualTo("POST");
        assertThat(result.expiresAt()).isAfter(NOW);
    }

    @Test
    void aBannerRequestUsesTheBannersFolder() {
        var result = service.handle(new RequestImageUploadUseCase.Command(7, "BANNER"));

        assertThat(storage.permits.get(0).slot()).isEqualTo(ImageSlot.BANNER);
        assertThat(result.fields()).containsEntry("public_id", "chatter/profile/banners/7/upload-1");
    }
}
