package com.minewaku.chatter.profile.application.port.inbound.command.asset.usecase;

import java.util.Map;

import com.minewaku.chatter.profile.application.port.inbound.shared.handler.UseCaseHandler;

public interface HandleUploadNotificationUseCase extends UseCaseHandler<HandleUploadNotificationUseCase.Command, Void> {

    public record Command(
        Map<String, String> headers,
        Map<String, Object> body 
    ) {}
}
