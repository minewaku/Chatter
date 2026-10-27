package com.minewaku.chatter.identityaccess.start.e2e;

import java.net.http.HttpHeaders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** One HTTP response: status, headers, and the raw body with a JSON view. */
public record ApiResponse(int status, HttpHeaders headers, String body) {

    public static final String REFRESH_COOKIE = "refresh_token";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public JsonNode json() {
        return MAPPER.readTree(body);
    }

    public String text(String field) {
        return json().get(field).asString();
    }

    /** The refresh credential set by this response, or {@code null} when it sets none (or clears it). */
    public String refreshCookie() {
        return headers.allValues("Set-Cookie").stream()
                .filter(cookie -> cookie.startsWith(REFRESH_COOKIE + "="))
                .map(cookie -> cookie.substring(REFRESH_COOKIE.length() + 1).split(";", 2)[0])
                .filter(value -> !value.isEmpty())
                .findFirst()
                .orElse(null);
    }

    public String header(String name) {
        return headers.firstValue(name).orElse(null);
    }
}
