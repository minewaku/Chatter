package com.minewaku.chatter.profilenext.profile.internal.application.command;

import com.minewaku.chatter.profilenext.profile.api.command.RequestImageUploadUseCase;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageSlot;
import com.minewaku.chatter.profilenext.profile.internal.model.UploadPermission;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.ImageStorage;
import com.minewaku.chatter.profilenext.shared.port.UniqueStringIdGenerator;
import java.time.Clock;
import org.springframework.stereotype.Service;

/** Issues an upload permission for the caller's own slot only. Nothing is stored until the provider confirms. */
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
        UploadPermission permission = storage.permit(userId, slot, ids.nextId(), clock.instant());
        return new Result(permission.uploadUrl(), permission.method(), permission.fields(), permission.expiresAt());
    }
}
