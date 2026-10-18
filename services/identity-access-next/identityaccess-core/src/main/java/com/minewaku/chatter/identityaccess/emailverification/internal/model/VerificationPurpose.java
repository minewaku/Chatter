package com.minewaku.chatter.identityaccess.emailverification.internal.model;

/**
 * What a proof of mailbox ownership may complete. A proof of one purpose is stored apart from the other, so a token
 * issued for one can never be found, redeemed, or invalidated through the other.
 */
public enum VerificationPurpose {
    REGISTRATION,
    EMAIL_CHANGE
}
