package com.minewaku.chatter.domain.port.in;

public interface UseCaseFallbackHandler<C, R> {
	R fallback(C command, Throwable throwable );
}
