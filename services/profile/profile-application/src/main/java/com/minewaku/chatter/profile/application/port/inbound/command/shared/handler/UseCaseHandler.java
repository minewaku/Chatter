package com.minewaku.chatter.profile.application.port.inbound.command.shared.handler;

public interface UseCaseHandler<C, R> {
	R handle(C command);
}
