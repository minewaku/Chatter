package com.minewaku.chatter.profilenext.profile.internal.model;

/** A validated edit. A null part means "leave unchanged". */
public record ProfileEdit(DisplayName displayName, Bio bio) {

    public boolean isEmpty() {
        return displayName == null && bio == null;
    }
}
