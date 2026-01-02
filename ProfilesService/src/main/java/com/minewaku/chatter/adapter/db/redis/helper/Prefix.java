package com.minewaku.chatter.adapter.db.redis.helper;

public enum Prefix {
	CONFIRMATION_TOKEN_VALUE("confirmation_token:value:"),
	CONFIRMATION_TOKEN_LOOKUP("confirmation_token:lookup:"),
	REFRESH_TOKEN_VALUE("refresh_token:value:");

	private final String keyPrefix;

	Prefix(String keyPrefix) {
		this.keyPrefix = keyPrefix;
	}

	public String format(String id) {
		return keyPrefix + id;
	}
}

