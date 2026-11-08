package com.minewaku.chatter.profilenext.profile.internal.application.command;

import com.minewaku.chatter.profilenext.profile.api.command.RequestImageUploadUseCase;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageId;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.OwnedImage;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage.UploadPermission;
import com.minewaku.chatter.profilenext.shared.port.UniqueStringIdGenerator;
import java.time.Clock;
import org.springframework.stereotype.Service;

/**
 * Issues an upload permission for the user in the command and the requested slot, under a fresh image id. Writes
 * nothing: the profile changes only when the provider confirms the upload.
 */
@Service
public class RequestImageUploadService implements RequestImageUploadUseCase {

    private final ImageStorage storage;
    private final UniqueStringIdGenerator ids;
    private final Clock clock;

    public RequestImageUploadService(ImageStorage storage, UniqueStringIdGenerator ids, Clock clock) {
        this.storage = storage;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    public Result handle(Command command) {
        UserId userId = UserId.of(command.userId());
        ImageSlot slot = ImageSlot.valueOf(command.slot());
        ImageId imageId = ImageId.of(ids.nextId());
        UploadPermission permission = storage.permit(new OwnedImage(userId, slot, imageId), clock.instant());
        return new Result(permission.uploadUrl(), permission.method(), permission.fields(), permission.expiresAt());
    }
}
