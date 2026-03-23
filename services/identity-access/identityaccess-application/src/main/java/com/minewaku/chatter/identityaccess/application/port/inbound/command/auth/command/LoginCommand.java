package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command;

import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.DeviceInfo;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.Password;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record LoginCommand(
    @NonNull Email email,
    @NonNull Password password,
    @NonNull DeviceInfo deviceInfo
) {}
