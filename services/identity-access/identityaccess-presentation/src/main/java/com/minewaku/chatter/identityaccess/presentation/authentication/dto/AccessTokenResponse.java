package com.minewaku.chatter.identityaccess.presentation.authentication.dto;

/** The access credential, returned only in the response that issues it. The refresh credential travels as a cookie. */
public record AccessTokenResponse(String accessToken) {

    @Override
    public String toString() {
        return "AccessTokenResponse";
    }
}
