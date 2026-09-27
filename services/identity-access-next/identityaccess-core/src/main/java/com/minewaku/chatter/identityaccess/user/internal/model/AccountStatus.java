package com.minewaku.chatter.identityaccess.user.internal.model;

public enum AccountStatus {
    PENDING_VERIFICATION(false),
    ACTIVE(true),
    LOCKED(false),
    SUSPENDED(false),
    DELETED(false);

    private final boolean accessible;

    AccountStatus(boolean accessible) {
        this.accessible = accessible;
    }

    public boolean isAccessible() {
        return accessible;
    }
}
