package com.minewaku.chatter.filters;

import com.minewaku.chatter.config.RateLimitProperties;
import com.minewaku.chatter.exceptions.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int HTTP_STATUS_TOO_MANY_REQUESTS = 429;

    private final RateLimitProperties properties;

    private static class Counter {
        volatile long windowEpochSecond;
        final AtomicInteger count = new AtomicInteger(0);
    }

    private final Map<String, Counter> keyToCounter = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String key = resolveKey(request);
        long nowSec = Instant.now().getEpochSecond();
        Counter counter = keyToCounter.computeIfAbsent(key, k -> {
            Counter c = new Counter();
            c.windowEpochSecond = nowSec;
            return c;
        });

        synchronized (counter) {
            if (nowSec - counter.windowEpochSecond >= properties.getWindowSeconds()) {
                counter.windowEpochSecond = nowSec;
                counter.count.set(0);
            }
            int current = counter.count.incrementAndGet();
            if (current > properties.getRequestsPerMinute()) {
                response.setStatus(HTTP_STATUS_TOO_MANY_REQUESTS);
                response.setHeader("Retry-After", String.valueOf(properties.getWindowSeconds()));
                throw new RateLimitExceededException("Rate limit exceeded");
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        String header = request.getHeader(properties.getKeyHeader());
        if (header != null && !header.isBlank()) {
            return header;
        }
        if (properties.isUseIpFallback()) {
            return request.getRemoteAddr();
        }
        return "global";
    }
}