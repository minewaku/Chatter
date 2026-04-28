package com.minewaku.chatter.profile.presentation.web.request;

public record GenerateUploadSignatureRequest(
    String namespace,
    String fileHash
) {
}