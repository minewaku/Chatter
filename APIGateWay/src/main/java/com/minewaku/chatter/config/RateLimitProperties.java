package com.minewaku.chatter.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "gateway.ratelimit")
@Validated
public class RateLimitProperties {

    private boolean enabled = true;

    @Min(1)
    private int requestsPerMinute = 120;

    @Min(1)
    private int windowSeconds = 60;

    private String keyHeader = "X-API-KEY";

    private boolean useIpFallback = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public void setRequestsPerMinute(int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public String getKeyHeader() {
        return keyHeader;
    }

    public void setKeyHeader(String keyHeader) {
        this.keyHeader = keyHeader;
    }

    public boolean isUseIpFallback() {
        return useIpFallback;
    }

    public void setUseIpFallback(boolean useIpFallback) {
        this.useIpFallback = useIpFallback;
    }
}