package com.minewaku.chatter.web.request;

import java.util.Map;

public record PdpRequest(
        String resourceType, 
        String resourceId, 
        String action,               
        Map<String, Object> resourceAttrs
    ) {}
