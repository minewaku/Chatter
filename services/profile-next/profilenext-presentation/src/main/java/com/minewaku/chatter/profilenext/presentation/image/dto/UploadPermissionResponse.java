package com.minewaku.chatter.profilenext.presentation.image.dto;

import com.minewaku.chatter.profilenext.profile.api.command.RequestImageUploadUseCase;
import java.time.Instant;
import java.util.Map;

/**
 * Send {@code method} to {@code uploadUrl} with {@code headers} and the file as the body; for providers that take a form
 * upload, also send {@code fields}. Hand {@code imageId} back to the commit call once the upload has finished.
 */
public record UploadPermissionResponse(
        String imageId,
        String uploadUrl,
        String method,
        Map<String, String> headers,
        Map<String, String> fields,
        Instant expiresAt) {

    public static UploadPermissionResponse from(RequestImageUploadUseCase.Result result) {
        return new UploadPermissionResponse(
                result.imageId(), result.uploadUrl(), result.method(), result.headers(), result.fields(), result.expiresAt());
    }
}
