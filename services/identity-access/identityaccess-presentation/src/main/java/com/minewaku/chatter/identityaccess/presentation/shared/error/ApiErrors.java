package com.minewaku.chatter.identityaccess.presentation.shared.error;

import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/** Builds the standard error response for outcomes a controller decides itself (as opposed to thrown failures). */
public final class ApiErrors {

    private ApiErrors() {}

    public static ResponseEntity<ApiError> response(ErrorCode code) {
        return ResponseEntity.status(code.status()).body(ApiError.of(code));
    }

    /** Same body, plus {@code Retry-After} in whole seconds: rounded up, and never less than one. */
    public static ResponseEntity<ApiError> response(ErrorCode code, Duration retryAfter) {
        long seconds = Math.max(1, (retryAfter.toMillis() + 999) / 1000);
        return ResponseEntity.status(code.status())
                .header(HttpHeaders.RETRY_AFTER, Long.toString(seconds))
                .body(ApiError.of(code));
    }
}
