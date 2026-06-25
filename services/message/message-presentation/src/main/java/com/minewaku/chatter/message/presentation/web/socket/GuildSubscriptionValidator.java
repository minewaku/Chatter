package com.minewaku.chatter.message.presentation.web.socket;

import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import com.minewaku.chatter.message.application.port.inbound.query.CheckGuildAccessUseCase;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class GuildSubscriptionValidator implements TopicSubscriptionValidator {

    private final AntPathMatcher pathMatcher;
    private final CheckGuildAccessUseCase checkGuildAccessUseCase;
    private static final String GUILD_TOPIC_PATTERN = "/topic/guilds/{guildId}";

    @Override
    public boolean supports(String destination) {
        return destination != null && pathMatcher.match(GUILD_TOPIC_PATTERN, destination);
    }

    @Override
    public void validate(String destination, String userIdStr) {
        Map<String, String> variables = pathMatcher.extractUriTemplateVariables(GUILD_TOPIC_PATTERN, destination);
        String guildIdStr = variables.get("guildId");

        CheckGuildAccessUseCase.Command command = new CheckGuildAccessUseCase.Command(
            new GuildId(Long.parseLong(guildIdStr)),
            new UserId(Long.parseLong(userIdStr))
        );
            
        boolean hasAccess = checkGuildAccessUseCase.handle(command);
        if (!hasAccess) {
            throw new AccessDeniedException("You are not allowed to subscribe to this guild");
        }
    }
}
