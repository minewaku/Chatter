package com.minewaku.chatter.profile.domain.model.file.model;

import lombok.NonNull;

public enum Namespace {
    USER_AVATARS,
    USER_BANNERS;

    public static Namespace fromValue(@NonNull String value) {
        for(Namespace namespace : Namespace.values()) {
            if(namespace.name().equals(value)) {
                return namespace;
            }
        }
        throw new IllegalArgumentException("No matching namespace for value: " + value);
    }
}
