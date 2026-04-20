package com.minewaku.chatter.profile.application.port.inbound.shared.handler;

public interface UseCaseFallbackHandler<C, R> {
	R fallback(C command, Throwable throwable );
}
