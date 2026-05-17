package com.minewaku.chatter.message.application.port.inbound.command.message;

import java.util.Map;

import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;

public interface HandleUploadAttachmentNotificationUseCase extends UseCaseHandler<HandleUploadAttachmentNotificationUseCase.Command, Void> {

    public record Command(
        Map<String, String> headers,
        Map<String, Object> body 
    ) {}
}
