package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command;

import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.DeviceInfo;

public record RefreshTokenCommand(String refreshToken, DeviceInfo deviceInfo) {

}
