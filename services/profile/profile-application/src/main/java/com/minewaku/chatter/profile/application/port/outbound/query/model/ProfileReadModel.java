package com.minewaku.chatter.profile.application.port.outbound.query.model;

public record ProfileReadModel(
    Long id,
    String username,
    String displayName,
    String bio,
    String avatarHash,
    String bannerHash){
}
