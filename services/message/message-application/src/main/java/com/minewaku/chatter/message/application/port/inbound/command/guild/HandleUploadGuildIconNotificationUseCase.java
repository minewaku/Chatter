package com.minewaku.chatter.message.application.port.inbound.command.guild;

import java.util.Map;

import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;

public interface HandleUploadGuildIconNotificationUseCase extends UseCaseHandler<HandleUploadGuildIconNotificationUseCase.Command, Void> {

    public record Command(
        Map<String, String> headers,
        Map<String, Object> body 
    ) {}
}
