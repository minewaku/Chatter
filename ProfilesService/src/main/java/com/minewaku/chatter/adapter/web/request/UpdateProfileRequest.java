package com.minewaku.chatter.adapter.web.request;

public record UpdateProfileRequest(
    String displayName,
    String bio
) {}
