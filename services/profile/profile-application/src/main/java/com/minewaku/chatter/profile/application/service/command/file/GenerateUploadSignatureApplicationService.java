package com.minewaku.chatter.profile.application.service.command.file;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.GenerateUploadSignatureUseCase;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class GenerateUploadSignatureApplicationService implements GenerateUploadSignatureUseCase {

    private final AssetStorage assetStorage;

    public GenerateUploadSignatureApplicationService(AssetStorage assetStorage) {
        this.assetStorage = assetStorage;
    }

    @Override
    public AssetStorage.Response handle(Command command) {
        log.info("receive the fucking link! ");
        AssetStorage.Response response = assetStorage.generateUploadSignature(command.namespace());
        return response;
    }
    
}
