package com.minewaku.chatter.identityaccess.user.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;
import java.time.LocalDate;

public interface RegisterUserUseCase extends UseCaseHandler<RegisterUserUseCase.Command, RegisterUserUseCase.Result> {

    record Command(String email, String username, LocalDate birthday, String password) {}

    sealed interface Result permits Registered, AccountAlreadyExists, VerificationPending {}

    /** {@code verificationMailSent} is false when the account exists but the mail could not be sent now. */
    record Registered(long userId, boolean verificationMailSent) implements Result {}

    record AccountAlreadyExists() implements Result {}

    record VerificationPending() implements Result {}
}
