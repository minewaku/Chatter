package com.minewaku.chatter.message.application.port.inbound.query;

import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

import lombok.NonNull;

public interface CheckChannelAccessUseCase extends UseCaseHandler<CheckChannelAccessUseCase.Command, Boolean> {

    public record Command(
        @NonNull ChannelId channelId,
        @NonNull UserId userId
    ) {}
}