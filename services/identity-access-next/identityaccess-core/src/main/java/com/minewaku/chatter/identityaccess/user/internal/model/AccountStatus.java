package com.minewaku.chatter.identityaccess.user.internal.model;

public enum AccountStatus {

    /**
     * The account has been created but has not completed the initial
     * verification process yet.
     *
     * <p>The account cannot access protected account functionality until
     * verification succeeds and the status transitions to {@link #ACTIVE}.
     */
    PENDING_VERIFICATION(false),

    /**
     * The account has completed verification and is allowed to operate normally.
     *
     * <p>This is the only accessible account status.
     */
    ACTIVE(true),

    /**
     * The account is temporarily blocked for security reasons.
     *
     * <p>A locked account cannot access protected account functionality.
     * It may return to {@link #ACTIVE} through an explicit unlock operation.
     */
    LOCKED(false),

    /**
     * The account is administratively suspended.
     *
     * <p>A suspended account cannot access protected account functionality.
     * It may return to {@link #ACTIVE} through an explicit reinstate operation.
     */
    SUSPENDED(false),

    /**
     * The account has been soft-deleted.
     *
     * <p>This is a terminal state. A deleted account cannot be restored or
     * transitioned back to another status.
     */
    DELETED(false);

    private final boolean accessible;

    AccountStatus(boolean accessible) {
        this.accessible = accessible;
    }

    public boolean isAccessible() {
        return accessible;
    }
}
