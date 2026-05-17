package com.minewaku.chatter.message.application.service.command.guild;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.message.application.port.inbound.command.guild.GenerateUploadGuildIconSignatureUseCase;
import com.minewaku.chatter.message.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.message.domain.model.asset.model.Namespace;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class GenerateUploadGuildIconSignatureApplicationService implements GenerateUploadGuildIconSignatureUseCase {

    private final AssetStorage assetStorage;

    @Override
    public AssetStorage.UploadSignature handle(Command command) {
        Namespace namespace = Namespace.GUILD_ICON;

        Map<String, Object> params = Map.of("userId", command.userId().getValue(), "guildId", command.guildId().getValue());
        return assetStorage.generateUploadSignature(namespace, params);
    }
}
