package com.minewaku.chatter.profilenext.presentation.shared.messaging;

/**
 * Handles one type of message, named by the {@code eventType} header. A new message type is one new implementation of
 * this (a Spring bean) and nothing else in this package changes.
 *
 * @param <M> the shape the payload is parsed into
 */
public interface EventHandler<M> {

    /** The value of the {@code eventType} header this handler is responsible for. */
    String eventType();

    /** Reads the payload; anything unreadable must be an {@link UnreadableMessageException}. */
    M parse(String payload);

    /** Does the work; any other exception is retried and then dead-lettered by the container's error handler. */
    void handle(M message);
}
