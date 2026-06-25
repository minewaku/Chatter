package com.minewaku.chatter.profile.presentation.web.socket;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class ProfileSubscriptionValidator implements TopicSubscriptionValidator {

    private final AntPathMatcher pathMatcher;
    private static final String PROFILE_TOPIC_PATTERN = "/topic/profiles/{profileId}";

    @Override
    public boolean supports(String destination) {
        return destination != null && pathMatcher.match(PROFILE_TOPIC_PATTERN, destination);
    }

    @Override
    public void validate(String destination, String userIdStr) {
        Map<String, String> variables = pathMatcher.extractUriTemplateVariables(PROFILE_TOPIC_PATTERN, destination);
        String profileIdStr = variables.get("profileId");

        log.debug("User {} successfully authenticated and subscribed to profile {}", userIdStr, profileIdStr);
    }
}

