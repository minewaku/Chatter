package com.minewaku.chatter.identityaccess.shared.handler;

@FunctionalInterface
public interface UseCaseHandler<C, R> {

    R handle(C command);
}
