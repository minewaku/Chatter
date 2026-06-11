package com.minewaku.chatter.message.application.port.inbound.command.asset;

import java.util.Map;

import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;

public interface HandleUploadNotificationUseCase extends UseCaseHandler<HandleUploadNotificationUseCase.Command, Void> {

    public record Command(
        Map<String, String> headers,
        Map<String, Object> body 
    ) {}
}
