package com.minewaku.chatter.identityaccess.infrastructure.persistence.redis.helper;

public enum Prefix {
	CONFIRMATION_TOKEN_VALUE("confirmation_token:value:"),
	CONFIRMATION_TOKEN_LOOKUP("confirmation_token:lookup:"),
	SESSION_VALUE("session:value:"),
	SESSION_LOOKUP("session:lookup:");

	private final String keyPrefix;

	Prefix(String keyPrefix) {
		this.keyPrefix = keyPrefix;
	}

	public String format(String id) {
		return keyPrefix + id;
	}
}

