package com.minewaku.chatter.profile.infrastructure.cache.helper;

public enum Prefix {
	PROFILE("profileS:");

	private final String keyPrefix;

	Prefix(String keyPrefix) {
		this.keyPrefix = keyPrefix;
	}

	public String format(String id) {
		return keyPrefix + id;
	}
}

