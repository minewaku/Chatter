package com.minewaku.chatter.identityaccess.presentation.authentication;

/** The access and refresh credentials, returned only in the response that issues them. */
public record TokenPairResponse(String accessToken, String refreshToken) {

    @Override
    public String toString() {
        return "TokenPairResponse";
    }
}
