package com.minewaku.chatter.profile.application.port.outbound.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.minewaku.chatter.profile.domain.model.file.model.Namespace;

public interface AssetStorage {
    
    void upload(AssetStorage.StorableFile storableFile, String fileHash);
    Response generateUploadSignature(Namespace namespace);
    void delete(String fileHash);

    public interface StorableFile {
        InputStream openStream() throws IOException; 
        Namespace getNamespace();
    }

    public static record Response(
        String uploadUrl,
        String httpMethod,
        Map<String, Object> payload
    ) {}
}