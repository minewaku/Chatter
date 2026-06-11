package com.minewaku.chatter.message.application.service.command.message;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.port.inbound.command.asset.GenerateUploadSignatureUseCase;
import com.minewaku.chatter.message.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.message.domain.model.asset.model.Namespace;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("generateUploadAttachmentSignatureUseCase")
@AllArgsConstructor
public class GenerateAttachmentIconSignatureApplicationService implements GenerateUploadSignatureUseCase {

    private final AssetStorage assetStorage;

    @Override
    public AssetStorage.UploadSignature handle(Command command) {
        Namespace namespace = Namespace.ATTACHMENT;

        log.info("receive the fucking link! ");
        AssetStorage.UploadSignature response = assetStorage.generateUploadSignature(
            namespace,
            command.params());
        
        return response;
    }
}
