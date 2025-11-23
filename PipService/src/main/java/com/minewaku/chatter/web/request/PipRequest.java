package com.minewaku.chatter.web.request;

import java.util.Map;
import java.util.Set;

public record PipRequest(
    String resourceId,           
    Map<String, Object> resourceAttrs,
    Set<String> requestedAttributes
) {}
