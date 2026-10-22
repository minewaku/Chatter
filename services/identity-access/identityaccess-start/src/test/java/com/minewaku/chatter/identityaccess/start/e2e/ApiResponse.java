package com.minewaku.chatter.identityaccess.start.e2e;

import java.net.http.HttpHeaders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** One HTTP response: status, headers, and the raw body with a JSON view. */
public record ApiResponse(int status, HttpHeaders headers, String body) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public JsonNode json() {
        return MAPPER.readTree(body);
    }

    public String text(String field) {
        return json().get(field).asString();
    }

    public String header(String name) {
        return headers.firstValue(name).orElse(null);
    }
}
