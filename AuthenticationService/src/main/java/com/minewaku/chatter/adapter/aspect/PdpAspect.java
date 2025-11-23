package com.minewaku.chatter.adapter.aspect;

import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.minewaku.chatter.adapter.annotation.Attribute;
import com.minewaku.chatter.adapter.annotation.PdpCheck;
import com.minewaku.chatter.adapter.service.IPdpService;

import groovy.util.logging.Slf4j;

@Aspect
@Component
@Slf4j
public class PdpAspect {

    private static final Logger log = LoggerFactory.getLogger(PdpAspect.class);

    private final IPdpService pdpService;
    private final SpelExpressionParser parser;

    public PdpAspect(
            IPdpService pdpService,
            SpelExpressionParser parser) {
        
        this.pdpService = pdpService;
        this.parser = parser;
    }

    @Around("@annotation(PdpCheck)")
    public Object authorize(ProceedingJoinPoint joinPoint, PdpCheck PdpCheck) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
            context.setVariable("p" + i, args[i]);
        }

        try {
            String resourceId = evaluateSpel(PdpCheck.resourceIdParam(), context, "resourceIdParam");
            Map<String, Object> resourceAttrs = evaluateAttributes(PdpCheck.resourceAttrs(), context);

            JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
            Jwt jwt = auth.getToken();
            String principal = jwt.getSubject();

            boolean isAuthorized = pdpService.isAccessAllowed(
                PdpCheck.resourceType(),
                resourceId,
                PdpCheck.action(),
                resourceAttrs
            );


            if (!isAuthorized) {
                log.warn("Access denied for principal '{}' on action '{}' for resource '{}:{}'",
                    principal, PdpCheck.action(), PdpCheck.resourceType(), resourceId);
                throw new AccessDeniedException(
                    String.format("Access denied for action '%s' on resource '%s'", PdpCheck.action(), PdpCheck.resourceType())
                );
            }


            return joinPoint.proceed();

        } catch (Exception e) {
            log.error("Pdp authorization failed: {}", e.getMessage(), e);
            throw new AccessDeniedException("Authorization failed due to configuration error");
        }
    }


    private String evaluateSpel(String expression, EvaluationContext context, String fieldName) {
        if (expression == null || expression.isBlank()) {
            return null;
        }

        try {
            Expression exp = parser.parseExpression(expression);
            Object value = exp.getValue(context);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("Failed to evaluate SpEL expression for {}: '{}'. Error: {}", fieldName, expression, e.getMessage());
            throw new IllegalArgumentException("Invalid SpEL in " + fieldName + ": " + expression, e);
        }
    }


    private Map<String, Object> evaluateAttributes(Attribute[] attributes, EvaluationContext context) {
        Map<String, Object> result = new HashMap<>();
        if (attributes == null || attributes.length == 0) {
            return result;
        }

        for (Attribute attr : attributes) {
            String key = attr.key();
            String spel = attr.value();

            if (spel == null || spel.isBlank()) {
                continue;
            }

            try {
                Expression exp = parser.parseExpression(spel);
                Object value = exp.getValue(context);
                if (value != null) {
                    result.put(key, value);
                }
            } catch (Exception e) {
                log.warn("Failed to evaluate attribute key='{}' with SpEL='{}': {}", key, spel, e.getMessage());
                // Không throw → vẫn cho phép tiếp tục (attr bị bỏ qua)
            }
        }
        return result;
    }
}