package com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase;

import java.util.Map;

import com.minewaku.chatter.profile.application.port.inbound.shared.handler.UseCaseHandler;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;

public interface GenerateUploadSignatureUseCase extends UseCaseHandler<GenerateUploadSignatureUseCase.Command, AssetStorage.UploadSignature> {
    
    public record Command(Map<String, Object> params) {

    }
}
