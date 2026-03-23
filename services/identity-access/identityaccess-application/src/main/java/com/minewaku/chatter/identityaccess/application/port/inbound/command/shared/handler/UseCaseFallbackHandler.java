package com.minewaku.chatter.identityaccess.application.port.inbound.command.shared.handler;

public interface UseCaseFallbackHandler<C, R> {
	R fallback(C command, Throwable throwable );
}
