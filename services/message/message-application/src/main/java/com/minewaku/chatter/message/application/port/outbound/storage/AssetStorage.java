package com.minewaku.chatter.message.application.port.outbound.storage;

import java.util.Map;

import com.minewaku.chatter.message.domain.model.asset.model.Namespace;

public interface AssetStorage {
    
    UploadSignature generateUploadSignature(Namespace namespace, Map<String, Object> params);
    void delete(Namespace namespace, String fileHash);
    UploadResult handleUploadNotification(Map<String, String> headers, Map<String, Object> body);
    void commitUpload(Namespace namespace, String fileHash);

    public record UploadSignature(
        String uploadUrl,
        String httpMethod,
        Map<String, Object> payload
    ) {}

    public record UploadResult (
        String fileHash,
        String namespace,
        Map<String, Object> context,
        String contentType,
        String fileName,
        Integer fileSize
    ) {}
}