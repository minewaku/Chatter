package com.minewaku.chatter.profile.presentation.web.api.request;

public record GenerateUploadSignatureRequest(
    String namespace,
    String fileHash
) {
}