package com.minewaku.chatter.profilenext.profile.api.command;

import com.minewaku.chatter.profilenext.shared.handler.UseCaseHandler;
import java.time.Instant;

public interface DeleteProfileDataUseCase
        extends UseCaseHandler<DeleteProfileDataUseCase.Command, DeleteProfileDataUseCase.Result> {

    record Command(String eventId, long userId, Instant occurredAt) {}

    record Result() {}
}
