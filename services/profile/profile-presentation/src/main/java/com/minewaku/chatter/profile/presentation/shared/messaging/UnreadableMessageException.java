package com.minewaku.chatter.profile.presentation.shared.messaging;

/** A message that can never be processed, however often it is retried. It goes straight to the dead-letter topic. */
public class UnreadableMessageException extends RuntimeException {

    public UnreadableMessageException(String message) {
        super(message);
    }

    public UnreadableMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
