package com.minewaku.chatter.profile.profile.api.exception;

/**
 * A submitted display name or bio broke a rule. {@code field} is the request field, {@code rule} a stable name such as
 * {@code display_name_length}, and the message states the real limits. It never contains the submitted value.
 */
public class InvalidProfileValueException extends IllegalArgumentException {

    private final String field;
    private final String rule;

    public InvalidProfileValueException(String field, String rule, String message) {
        super(message);
        this.field = field;
        this.rule = rule;
    }

    public String field() {
        return field;
    }

    public String rule() {
        return rule;
    }
}
