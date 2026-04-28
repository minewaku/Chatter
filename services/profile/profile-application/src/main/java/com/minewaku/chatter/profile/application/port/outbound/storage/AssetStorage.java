package com.minewaku.chatter.profile.application.port.outbound.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.minewaku.chatter.profile.domain.model.file.model.AssetDimension;
import com.minewaku.chatter.profile.domain.model.file.model.Namespace;

public interface AssetStorage {
    
    UploadSignature generateUploadSignature(Namespace namespace, Map<String, Object> params);
    void delete(Namespace namespace, String fileHash, Map<String, Object> params);
    UploadResult handleUploadNotification(Map<String, String> headers, Map<String, Object> body);
    void commitUpload(Namespace namespace, String fileHash, Map<String, Object> params);

    //RECHECK: NO MORE NEED FOR INPUT STREAM SINCE WE ARE USING PRESIGNED URL INSTEAD
    public interface StorableFile {
        InputStream openStream() throws IOException; 
        Namespace getNamespace();
    }

    public record UploadSignature(
        String uploadUrl,
        String httpMethod,
        Map<String, Object> payload
    ) {}

    public record UploadResult (
        String fileHash,
        Namespace namespace,
        Map<String, Object> context,
        AssetDimension dimension,
        Integer fileSize
    ) {}
}