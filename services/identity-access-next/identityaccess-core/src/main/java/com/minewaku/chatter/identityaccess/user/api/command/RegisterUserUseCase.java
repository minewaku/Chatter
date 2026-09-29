package com.minewaku.chatter.identityaccess.user.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;
import java.time.LocalDate;

public interface RegisterUserUseCase extends UseCaseHandler<RegisterUserUseCase.Command, RegisterUserUseCase.Result> {

    record Command(String email, String username, LocalDate birthday, String password) {}

    sealed interface Result permits Registered, AccountAlreadyExists, VerificationPending {}

    record Registered(long userId) implements Result {}

    record AccountAlreadyExists() implements Result {}

    record VerificationPending() implements Result {}
}
