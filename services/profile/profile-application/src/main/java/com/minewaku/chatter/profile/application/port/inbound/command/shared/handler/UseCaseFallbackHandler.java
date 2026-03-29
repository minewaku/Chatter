package com.minewaku.chatter.profile.application.port.inbound.command.shared.handler;

public interface UseCaseFallbackHandler<C, R> {
	R fallback(C command, Throwable throwable );
}
