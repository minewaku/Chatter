package com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase;

import java.util.Map;

import com.minewaku.chatter.profile.application.port.inbound.shared.handler.UseCaseHandler;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.profile.domain.model.file.model.Namespace;

public interface GenerateUploadSignatureUseCase extends UseCaseHandler<GenerateUploadSignatureUseCase.Command, AssetStorage.UploadSignature> {
    
    public static record Command(Namespace namespace, Map<String, Object> params) {

    }
}
