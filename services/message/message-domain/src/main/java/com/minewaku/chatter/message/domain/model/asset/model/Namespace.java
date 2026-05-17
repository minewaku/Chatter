package com.minewaku.chatter.message.domain.model.asset.model;

import com.minewaku.chatter.message.domain.sharedkernel.exception.DomainValidationException;

import lombok.NonNull;

public enum Namespace {
    GUILD_ICON,
    ATTACHMENT;

    public static Namespace fromValue(@NonNull String value) {
        for(Namespace namespace : Namespace.values()) {
            if(namespace.name().equals(value)) {
                return namespace;
            }
        }
        throw new DomainValidationException("No matching namespace for value: " + value);
    }
}
