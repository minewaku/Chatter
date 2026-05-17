package com.minewaku.chatter.message.presentation.web.api.request.guild;

import java.time.Duration;

import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

public record CreateInviteRequest (
    GuildId guildId,
    UserId userId,
    Duration duration,
    int maxUses
){
}  
