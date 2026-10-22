package com.minewaku.chatter.identityaccess.user.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface ConfirmEmailChangeUseCase
        extends UseCaseHandler<ConfirmEmailChangeUseCase.Command, ConfirmEmailChangeUseCase.Result> {

    record Command(String token) {}

    sealed interface Result permits Confirmed, AlreadyConfirmed, InvalidOrExpired {}

    record Confirmed(long userId) implements Result {}

    record AlreadyConfirmed() implements Result {}

    record InvalidOrExpired() implements Result {}
}
