package com.minewaku.chatter.adapter.service;

import java.util.Map;

public interface IPdpService {
    boolean isAccessAllowed(String resourceType, String resourceId, String action, Map<String, Object> resourceAttrs);
}
