package com.minewaku.chatter.utils;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class HttpClientUtil {
	
    public String getClientIP(ServerHttpRequest request) {
        String xfHeader = request.getHeaders().getFirst("X-Forwarded-For");
        if (xfHeader == null){
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return xfHeader.split(",")[0];
    }
}
