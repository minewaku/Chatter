package com.minewaku.chatter.domain.port.in.user;

import com.minewaku.chatter.domain.port.in.UseCaseHandler;
import com.minewaku.chatter.domain.value.id.UserId;

public interface LockUserUseCase extends UseCaseHandler<UserId, Void> {
}
