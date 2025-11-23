package com.minewaku.chatter.constants;

import org.springframework.web.server.ServerWebExchange;

public enum ExchangeAttr {
    USER_ID, USERNAME, ROLES, TENANT_ID, TRACE_ID;

    public String key() {
        return "REQUEST_ATTR_" + name();
    }

    // Optional: helper methods
    public void put(ServerWebExchange exchange, Object value) {
        exchange.getAttributes().put(key(), value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(ServerWebExchange exchange) {
        return (T) exchange.getAttribute(key());
    }
}
