package com.minewaku.chatter.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String traceId = request.getHeader("X-Trace-Id");
        log.info("Incoming {} {} traceId={} from {}", method, uri, traceId, request.getRemoteAddr());
        try {
            filterChain.doFilter(request, response);
        } finally {
            long timeMs = System.currentTimeMillis() - start;
            log.info("Completed {} {} -> {} in {} ms", method, uri, response.getStatus(), timeMs);
        }
    }
}