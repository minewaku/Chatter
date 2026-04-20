package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command;

import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.Password;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record ChangePasswordCommand(
    @NonNull UserId userId,
    @NonNull Password password,
    @NonNull Password newPassword
) {}