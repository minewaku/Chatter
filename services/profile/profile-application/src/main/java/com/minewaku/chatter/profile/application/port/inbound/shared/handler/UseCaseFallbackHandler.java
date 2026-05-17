package com.minewaku.chatter.profile.application.port.inbound.shared.handler;

//RECHECK: U MAY NOT NEED THIS
public interface UseCaseFallbackHandler<C, R> {
	R fallback(C command, Throwable throwable );
}
