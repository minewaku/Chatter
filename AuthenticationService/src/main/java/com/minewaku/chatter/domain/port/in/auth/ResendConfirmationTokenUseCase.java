package com.minewaku.chatter.domain.port.in.auth;

import com.minewaku.chatter.domain.port.in.UseCaseHandler;
import com.minewaku.chatter.domain.value.Email;

public interface ResendConfirmationTokenUseCase extends UseCaseHandler<Email, Void> {
}
