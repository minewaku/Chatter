package com.minewaku.chatter.message.application.port.inbound.command.guild;

import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;
import com.minewaku.chatter.message.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.message.domain.model.guild.model.GuildId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

public interface GenerateUploadGuildIconSignatureUseCase extends UseCaseHandler<GenerateUploadGuildIconSignatureUseCase.Command, AssetStorage.UploadSignature> {
    
    public record Command(
        GuildId guildId,
        UserId userId
    ) {}
    
}
