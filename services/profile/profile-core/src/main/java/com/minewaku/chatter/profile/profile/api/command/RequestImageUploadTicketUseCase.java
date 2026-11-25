package com.minewaku.chatter.profile.profile.api.command;

import com.minewaku.chatter.profile.shared.handler.UseCaseHandler;
import java.time.Instant;
import java.util.Map;

public interface RequestImageUploadTicketUseCase
        extends UseCaseHandler<RequestImageUploadTicketUseCase.Command, RequestImageUploadTicketUseCase.Result> {

    /** {@code slot} is {@code AVATAR} or {@code BANNER}; {@code contentType} and {@code sizeBytes} describe the file about to be sent. */
    record Command(long userId, String slot, String contentType, long sizeBytes) {}

    /**
     * {@code imageId} is what the client hands back to commit. Send {@code method} to {@code uploadUrl} with
     * {@code headers}; for form uploads add {@code fields} and the file as multipart form data.
     */
    record Result(
            String imageId,
            String uploadUrl,
            String method,
            Map<String, String> headers,
            Map<String, String> fields,
            Instant expiresAt) {}
}
