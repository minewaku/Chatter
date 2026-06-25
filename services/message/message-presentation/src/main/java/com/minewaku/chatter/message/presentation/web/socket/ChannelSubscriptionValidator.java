package com.minewaku.chatter.message.presentation.web.socket;

import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import com.minewaku.chatter.message.application.port.inbound.query.CheckChannelAccessUseCase;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class ChannelSubscriptionValidator implements TopicSubscriptionValidator {

    private final AntPathMatcher pathMatcher;
    private final CheckChannelAccessUseCase checkChannelAccessUseCase;
    private static final String CHANNEL_TOPIC_PATTERN = "/topic/channels/{channelId}";

    @Override
    public boolean supports(String destination) {
        return destination != null && pathMatcher.match(CHANNEL_TOPIC_PATTERN, destination);
    }

    @Override
    public void validate(String destination, String userIdStr) {
        Map<String, String> variables = pathMatcher.extractUriTemplateVariables(CHANNEL_TOPIC_PATTERN, destination);
        String channelIdStr = variables.get("channelId");

        CheckChannelAccessUseCase.Command command = new CheckChannelAccessUseCase.Command(
            new ChannelId(Long.parseLong(channelIdStr)),
            new UserId(Long.parseLong(userIdStr))
        );

        boolean hasAccess = checkChannelAccessUseCase.handle(command);
        if (!hasAccess) {
            throw new AccessDeniedException("You are not allowed to subscribe to this channel");
        }
    }
}