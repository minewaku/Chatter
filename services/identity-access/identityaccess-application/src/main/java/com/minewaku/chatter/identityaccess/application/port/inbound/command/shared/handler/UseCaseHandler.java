package com.minewaku.chatter.identityaccess.application.port.inbound.command.shared.handler;

public interface UseCaseHandler<C, R> {
	R handle(C command);
}
