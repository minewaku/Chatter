package com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase;

import com.minewaku.chatter.profile.application.port.inbound.shared.handler.UseCaseHandler;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.profile.domain.model.file.model.Namespace;

public interface GenerateUploadSignatureUseCase extends UseCaseHandler<GenerateUploadSignatureUseCase.Command, AssetStorage.Response> {
    
    public static record Command(Namespace namespace) {

    }
}
