package com.minewaku.chatter.filters;

import com.minewaku.chatter.config.RateLimitProperties;
import com.minewaku.chatter.exceptions.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int HTTP_STATUS_TOO_MANY_REQUESTS = 429;

    private final RateLimitProperties properties;
    private final StringRedisTemplate stringRedisTemplate;

    public RateLimitFilter(RateLimitProperties properties, StringRedisTemplate stringRedisTemplate) {
        this.properties = properties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String username = resolveUsername(request);
        if (username == null || username.isBlank()) {
            // If no username, optionally fallback to IP or skip limiting
            username = properties.isUseIpFallback() ? request.getRemoteAddr() : "global";
        }

        long nowSec = Instant.now().getEpochSecond();
        long window = properties.getWindowSeconds();
        long windowStart = nowSec - (nowSec % window);
        String redisKey = String.format("ratelimit:%s:%d", username, windowStart);

        Long current = stringRedisTemplate.opsForValue().increment(redisKey);
        if (current != null && current == 1L) {
            stringRedisTemplate.expire(redisKey, window, TimeUnit.SECONDS);
        }

        int limit = properties.getRequestsPerMinute();
        if (current != null && current > limit) {
            response.setStatus(HTTP_STATUS_TOO_MANY_REQUESTS);
            response.setHeader("Retry-After", String.valueOf(window));
            throw new RateLimitExceededException("Rate limit exceeded");
        }

        filterChain.doFilter(request, response);
    }

    private String resolveUsername(HttpServletRequest request) {
        Object attr = request.getAttribute(AuthenticationFilter.REQUEST_ATTR_USERNAME);
        return attr != null ? String.valueOf(attr) : null;
    }
}