package com.minewaku.chatter.message.application.service.command.message;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.port.inbound.command.message.GenerateUploadAttachmentSignatureUseCase;
import com.minewaku.chatter.message.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.message.domain.model.asset.model.Namespace;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class GenerateUploadAttachmentSignatureApplicationService implements GenerateUploadAttachmentSignatureUseCase {

    private final AssetStorage assetStorage;

    @Override
    public AssetStorage.UploadSignature handle(Command command) {
        Namespace namespace = Namespace.ATTACHMENT;

        Map<String, Object> params = Map.of("userId", command.userId().getValue(), "channelId", command.channelId().getValue());
        return assetStorage.generateUploadSignature(namespace, params);
    }
}
