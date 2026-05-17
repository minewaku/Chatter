package com.minewaku.chatter.message.application.port.inbound.command.message;

import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;
import com.minewaku.chatter.message.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.message.domain.model.channel.model.ChannelId;
import com.minewaku.chatter.message.domain.model.message.model.MessageId;
import com.minewaku.chatter.message.domain.model.recipient.model.UserId;

public interface GenerateUploadAttachmentSignatureUseCase extends UseCaseHandler<GenerateUploadAttachmentSignatureUseCase.Command, AssetStorage.UploadSignature> {
    
    public record Command(
        ChannelId channelId,
        MessageId messageId,
        UserId userId
    ) {}
    
}
