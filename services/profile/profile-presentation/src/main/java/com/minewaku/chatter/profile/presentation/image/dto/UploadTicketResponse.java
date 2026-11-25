package com.minewaku.chatter.profile.presentation.image.dto;

import com.minewaku.chatter.profile.profile.api.command.RequestImageUploadTicketUseCase;
import java.time.Instant;
import java.util.Map;

/**
 * Send {@code method} to {@code uploadUrl} with {@code headers} and the file as the body; for providers that take a form
 * upload, also send {@code fields}. Hand {@code imageId} back to the commit call once the upload has finished.
 */
public record UploadTicketResponse(
        String imageId,
        String uploadUrl,
        String method,
        Map<String, String> headers,
        Map<String, String> fields,
        Instant expiresAt) {

    public static UploadTicketResponse from(RequestImageUploadTicketUseCase.Result result) {
        return new UploadTicketResponse(
                result.imageId(), result.uploadUrl(), result.method(), result.headers(), result.fields(), result.expiresAt());
    }
}
