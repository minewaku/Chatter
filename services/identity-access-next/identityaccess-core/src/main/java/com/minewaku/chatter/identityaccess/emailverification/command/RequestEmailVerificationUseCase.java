package com.minewaku.chatter.identityaccess.emailverification.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface RequestEmailVerificationUseCase
        extends UseCaseHandler<RequestEmailVerificationUseCase.Command, RequestEmailVerificationUseCase.Accepted> {
    record Command(String email) {}

    record Accepted() {}
}
