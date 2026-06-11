package com.minewaku.chatter.message.application.port.inbound.command.asset;

import java.util.Map;

import com.minewaku.chatter.message.application.port.inbound.shared.UseCaseHandler;
import com.minewaku.chatter.message.application.port.outbound.storage.AssetStorage;

public interface GenerateUploadSignatureUseCase extends UseCaseHandler<GenerateUploadSignatureUseCase.Command, AssetStorage.UploadSignature> {
    
    public record Command(
        Map<String, Object> params
    ) {}
}
