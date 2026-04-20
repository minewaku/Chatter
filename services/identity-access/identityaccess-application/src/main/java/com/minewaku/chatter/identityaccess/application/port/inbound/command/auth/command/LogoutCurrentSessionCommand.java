package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command;

import lombok.Builder;

@Builder
public record LogoutCurrentSessionCommand (
    String refreshToken
) {}
