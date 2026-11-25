package com.minewaku.chatter.profile.shared.handler;

@FunctionalInterface
public interface UseCaseHandler<C, R> {

    R handle(C command);
}
