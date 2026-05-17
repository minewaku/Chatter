package com.minewaku.chatter.message.application.port.inbound.shared;

public interface UseCaseHandler<C, R> {
	R handle(C command);
}
