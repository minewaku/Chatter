package com.minewaku.chatter.profilenext.presentation.image.dto;

import com.minewaku.chatter.profilenext.profile.api.command.RequestImageUploadUseCase;
import java.time.Instant;
import java.util.Map;

/** Send {@code fields} plus the file, as multipart form data, to {@code uploadUrl}. */
public record UploadPermissionResponse(String uploadUrl, String method, Map<String, String> fields, Instant expiresAt) {

    public static UploadPermissionResponse from(RequestImageUploadUseCase.Result result) {
        return new UploadPermissionResponse(result.uploadUrl(), result.method(), result.fields(), result.expiresAt());
    }
}
