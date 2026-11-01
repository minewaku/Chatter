package com.minewaku.chatter.profilenext.shared.handler;

@FunctionalInterface
public interface UseCaseHandler<C, R> {

    R handle(C command);
}
