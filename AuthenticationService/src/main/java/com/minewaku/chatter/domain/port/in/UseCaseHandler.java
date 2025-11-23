package com.minewaku.chatter.domain.port.in;

public interface UseCaseHandler<C, R> {
	R handle(C command);
}
