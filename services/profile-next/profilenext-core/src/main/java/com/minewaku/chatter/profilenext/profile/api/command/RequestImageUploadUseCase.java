package com.minewaku.chatter.profilenext.profile.api.command;

import com.minewaku.chatter.profilenext.shared.handler.UseCaseHandler;
import java.time.Instant;
import java.util.Map;

public interface RequestImageUploadUseCase
        extends UseCaseHandler<RequestImageUploadUseCase.Command, RequestImageUploadUseCase.Result> {

    /** {@code slot} is {@code AVATAR} or {@code BANNER}. */
    record Command(long userId, String slot) {}

    record Result(String uploadUrl, String method, Map<String, String> fields, Instant expiresAt) {}
}
