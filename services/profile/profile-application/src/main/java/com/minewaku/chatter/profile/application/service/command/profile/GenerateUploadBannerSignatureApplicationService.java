package com.minewaku.chatter.profile.application.service.command.profile;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.GenerateUploadSignatureUseCase;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.profile.domain.model.asset.model.Namespace;

import lombok.extern.log4j.Log4j2;

@Service("generateUploadBannerSignatureUseCase")
@Log4j2
public class GenerateUploadBannerSignatureApplicationService implements GenerateUploadSignatureUseCase {

    private final AssetStorage assetStorage;

    public GenerateUploadBannerSignatureApplicationService(AssetStorage assetStorage) {
        this.assetStorage = assetStorage;
    }

    @Override
    public AssetStorage.UploadSignature handle(Command command) {
        Namespace namespace = Namespace.USER_BANNERS;

        log.info("receive the fucking link! ");
        AssetStorage.UploadSignature response = assetStorage.generateUploadSignature(
            namespace,
            command.params());
        
        return response;
    }
    
}
