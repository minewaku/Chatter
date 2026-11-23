package com.minewaku.chatter.profilenext.profile.api.command;

import com.minewaku.chatter.profilenext.shared.handler.UseCaseHandler;
import java.time.Instant;

public interface DeleteProfileUseCase
        extends UseCaseHandler<DeleteProfileUseCase.Command, DeleteProfileUseCase.Result> {

    record Command(String eventId, long userId, Instant occurredAt) {}

    record Result() {}
}
