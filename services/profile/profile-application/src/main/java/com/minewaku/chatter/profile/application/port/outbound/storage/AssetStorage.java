package com.minewaku.chatter.profile.application.port.outbound.storage;

import java.io.InputStream;

import com.minewaku.chatter.profile.domain.model.file.model.Namespace;

public interface AssetStorage {
    void upload(AssetStorage.StorableFile storableFile, String fileHash);

    public interface StorableFile {
        InputStream getInputStream();
        Namespace getNamespace();
    }
}
