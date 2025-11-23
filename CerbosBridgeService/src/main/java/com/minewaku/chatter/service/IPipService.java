package com.minewaku.chatter.service;

import java.util.Map;
import java.util.Set;

public interface IPipService {
    PipResponse sendPipRequest(PipRequest request);

    public record PipRequest(
        String resourceId,           
        Map<String, Object> resourceAttrs,
        Set<String> requestedAttributes) {
    }

    public record PipResponse(
        boolean success,
        Map<String, Object> conditions) {
    }
}
