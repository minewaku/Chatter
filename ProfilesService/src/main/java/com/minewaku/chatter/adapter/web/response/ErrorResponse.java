package com.minewaku.chatter.adapter.web.response;

import java.time.Instant;

import lombok.Builder;

@Builder
public record ErrorResponse(String errorCode, String message, Instant timestamp) {
    public ErrorResponse(String errorCode, String message) {
        this(errorCode, message, Instant.now());
    }
}
