package com.minewaku.chatter.message.application.port.inbound.command.file;

import java.util.Map;

import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;
import com.minewaku.chatter.message.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.message.domain.model.asset.model.Namespace;

public interface GenerateUploadSignatureUseCase extends UseCaseHandler<GenerateUploadSignatureUseCase.Command, AssetStorage.UploadSignature> {
    
    public record Command(Namespace namespace, Map<String, Object> params) {

    }
}
