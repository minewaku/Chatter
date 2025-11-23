package com.minewaku.chatter.domain.port.in.user;

import com.minewaku.chatter.domain.port.in.UseCaseHandler;
import com.minewaku.chatter.domain.value.id.UserId;

public interface RestoreUserUseCase extends UseCaseHandler<UserId, Void> {
}
