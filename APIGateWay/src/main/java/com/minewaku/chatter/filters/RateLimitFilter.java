package com.minewaku.chatter.filters;

import com.minewaku.chatter.config.RateLimitProperties;
import com.minewaku.chatter.exceptions.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.distributed.bucket.ProxyBucketBuilder;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int HTTP_STATUS_TOO_MANY_REQUESTS = 429;
    private static final String KEY_PREFIX = "rate_limit:user:";

    private final RateLimitProperties properties;
    private final ProxyManager<String> proxyManager;

    public RateLimitFilter(RateLimitProperties properties, ProxyManager<String> proxyManager) {
        this.properties = properties;
        this.proxyManager = proxyManager;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String username = resolveUsername(request);
        if (username == null || username.isBlank()) {
            username = properties.isUseIpFallback() ? request.getRemoteAddr() : "global";
        }
        String key = KEY_PREFIX + username;

        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                .addLimit(buildBandwidth())
                .build();

        ProxyBucketBuilder<String> builder = proxyManager.builder().withKey(key);
        // inherit remaining tokens when configuration changes
        var bucket = builder.build(configSupplier, TokensInheritanceStrategy.proportionally());

        if (!bucket.tryConsume(1)) {
            response.setStatus(HTTP_STATUS_TOO_MANY_REQUESTS);
            response.setHeader("Retry-After", String.valueOf(properties.getWindowSeconds()));
            throw new RateLimitExceededException("Rate limit exceeded");
        }

        filterChain.doFilter(request, response);
    }

    private Bandwidth buildBandwidth() {
        int requests = properties.getRequestsPerMinute();
        int window = properties.getWindowSeconds();
        // Convert requests per minute into a window-based limit using greedy refill
        return Bandwidth.classic(requests, Refill.greedy(requests, Duration.ofSeconds(window)));
    }

    private String resolveUsername(HttpServletRequest request) {
        Object attr = request.getAttribute(AuthenticationFilter.REQUEST_ATTR_USERNAME);
        return attr != null ? String.valueOf(attr) : null;
    }
}