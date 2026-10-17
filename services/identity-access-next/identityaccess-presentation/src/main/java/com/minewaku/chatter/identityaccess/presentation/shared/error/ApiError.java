package com.minewaku.chatter.identityaccess.presentation.shared.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** The single JSON body returned for every non-success response. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String code, String message, Instant timestamp, List<FieldError> errors) {

    public record FieldError(String field, String message) {}

    public static ApiError of(ErrorCode code) {
        return new ApiError(code.status().value(), code.code(), code.message(), Instant.now(), null);
    }

    public static ApiError of(ErrorCode code, List<FieldError> errors) {
        return new ApiError(code.status().value(), code.code(), code.message(), Instant.now(), errors);
    }
}
