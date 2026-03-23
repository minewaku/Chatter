package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command;

import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.SessionId;

import lombok.Builder;

@Builder
public record LogoutCurrentSessionCommand (
    String refreshToken,
    SessionId currentSessionId
) {}
