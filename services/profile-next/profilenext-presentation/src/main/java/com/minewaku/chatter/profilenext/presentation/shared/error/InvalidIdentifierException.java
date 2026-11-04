package com.minewaku.chatter.profilenext.presentation.shared.error;

/** A path identifier that is not a positive whole number. Mapped to {@code 400 validation_failed}, never a 404. */
public class InvalidIdentifierException extends RuntimeException {

    private final String field;

    public InvalidIdentifierException(String field) {
        super("invalid identifier: " + field);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
