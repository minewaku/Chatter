package com.minewaku.chatter.web.request;

import java.util.Map;
import java.util.Set;

public record PipRequest(
    Map<String, Object> principalAttrs, 
    Set<String> conditions) {
}
