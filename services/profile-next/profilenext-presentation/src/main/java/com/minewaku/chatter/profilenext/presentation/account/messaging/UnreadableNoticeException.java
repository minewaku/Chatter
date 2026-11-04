package com.minewaku.chatter.profilenext.presentation.account.messaging;

/** A notice that can never be processed, however often it is retried. It goes straight to the dead-letter topic. */
public class UnreadableNoticeException extends RuntimeException {

    public UnreadableNoticeException(String message) {
        super(message);
    }

    public UnreadableNoticeException(String message, Throwable cause) {
        super(message, cause);
    }
}
