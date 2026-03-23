package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command;

import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.Password;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record ChangePasswordCommand(
    @NonNull Email email,
    @NonNull Password password,
    @NonNull Password newPassword
) {}