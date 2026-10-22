package com.minewaku.chatter.identityaccess.presentation.shared.validation;

/**
 * The password shape clients are told about, so a weak new password is reported against its field. It mirrors the
 * rule enforced by core (which stays the authority and still rejects anything it dislikes with a generic
 * {@code validation_failed}). Update both together.
 */
public final class PasswordRule {

    public static final String REGEX =
            "^(?=[^A-Z]*+[A-Z])(?=[^a-z]*+[a-z])(?=\\D*+\\d)(?=[^#?!@$%^&*-]*+[#?!@$%^&*-]).{8,}$";

    public static final String MESSAGE =
            "must be at least 8 characters with an uppercase letter, a lowercase letter, a digit and a special character";

    private PasswordRule() {}
}
