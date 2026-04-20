package com.minewaku.chatter.profile.application.port.outbound.query.model;

public record ProfileReadModel(
    long profileId,
    String username,
    String displayName,
    String bio,
    String avatarHash,
    String bannerHash){
}
