package com.minewaku.chatter.web.response;

import java.time.ZonedDateTime;

public record ErrorResponse(
    String message,
    ZonedDateTime timestamp
) {
}
