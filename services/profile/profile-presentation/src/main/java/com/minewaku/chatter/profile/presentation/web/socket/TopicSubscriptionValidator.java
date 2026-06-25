package com.minewaku.chatter.profile.presentation.web.socket;

public interface TopicSubscriptionValidator {
    boolean supports(String destination);
    void validate(String destination, String userIdStr);
}
