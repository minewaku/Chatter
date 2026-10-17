package com.minewaku.chatter.identityaccess.presentation.shared.error;

import org.springframework.http.ResponseEntity;

/** Builds the standard error response for outcomes a controller decides itself (as opposed to thrown failures). */
public final class ApiErrors {

    private ApiErrors() {}

    public static ResponseEntity<ApiError> response(ErrorCode code) {
        return ResponseEntity.status(code.status()).body(ApiError.of(code));
    }
}
