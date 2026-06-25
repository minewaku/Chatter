package com.minewaku.chatter.profile.presentation.web.socket;

import java.security.Principal;
import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionInterceptor implements ChannelInterceptor {

    private final List<TopicSubscriptionValidator> validators;
    private final JwtDecoder jwtDecoder;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        // 1. XỬ LÝ CONNECT
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {  
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                try {
                    Jwt jwt = jwtDecoder.decode(token);
                    
                    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
                    accessor.setUser(authentication);
                    
                } catch (JwtException e) {
                    throw new AccessDeniedException("Invalid Token: " + e.getMessage());
                }
            } else {
                throw new AccessDeniedException("Missing Bearer Token");
            }
        }

        // 2. XỬ LÝ SUBSCRIBE
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            
            if (destination != null) {
                for (TopicSubscriptionValidator validator : validators) {
                    if (validator.supports(destination)) {
                        Principal principal = accessor.getUser();
                        
                        if (!(principal instanceof JwtAuthenticationToken jwtAuth)) {
                            throw new AccessDeniedException("User not authenticated for topic");
                        }
                        String userIdStr = jwtAuth.getToken().getSubject();
                        
                        validator.validate(destination, userIdStr);
                        return message;
                    }
                }
            }
        }

        return message;
    }
}