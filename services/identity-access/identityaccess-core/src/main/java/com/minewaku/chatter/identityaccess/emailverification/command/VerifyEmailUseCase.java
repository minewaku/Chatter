package com.minewaku.chatter.identityaccess.emailverification.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface VerifyEmailUseCase extends UseCaseHandler<VerifyEmailUseCase.Command, VerifyEmailUseCase.Result> {
    record Command(String token) {}

    sealed interface Result permits Verified, InvalidOrExpired, AlreadyVerified {}

    record Verified(long userId) implements Result {}

    record InvalidOrExpired() implements Result {}

    record AlreadyVerified() implements Result {}
}
