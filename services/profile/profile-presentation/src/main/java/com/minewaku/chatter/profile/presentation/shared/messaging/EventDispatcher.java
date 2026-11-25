package com.minewaku.chatter.profile.presentation.shared.messaging;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Routes a record to the {@link EventHandler} named by its {@code eventType} header. One listener per topic calls this,
 * so every type on a topic is consumed in partition order by one consumer. A type nobody handles is skipped (other types
 * may share the topic); a record with no type at all can never be routed and is unreadable. Two handlers claiming one
 * type is a wiring mistake and stops the application from starting. The log lines carry the record's position, never
 * its payload.
 */
@Component
public class EventDispatcher {

    static final String EVENT_TYPE_HEADER = "eventType";

    private static final Logger log = LoggerFactory.getLogger(EventDispatcher.class);

    private final Map<String, EventHandler<?>> handlersByType;

    public EventDispatcher(List<EventHandler<?>> handlers) {
        Map<String, EventHandler<?>> byType = new HashMap<>();
        for (EventHandler<?> handler : handlers) {
            EventHandler<?> previous = byType.put(handler.eventType(), handler);
            if (previous != null) {
                throw new IllegalStateException("two handlers claim eventType " + handler.eventType() + ": "
                        + previous.getClass().getName() + " and " + handler.getClass().getName());
            }
        }
        this.handlersByType = Map.copyOf(byType);
    }

    public void dispatch(ConsumerRecord<String, String> record) {
        String eventType = eventType(record);
        if (eventType == null) {
            throw new UnreadableMessageException("the message has no " + EVENT_TYPE_HEADER + " header");
        }
        EventHandler<?> handler = handlersByType.get(eventType);
        if (handler == null) {
            log.debug(
                    "skipped record topic={} partition={} offset={} eventType={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    eventType);
            return;
        }
        handle(handler, record.value());
    }

    private static <M> void handle(EventHandler<M> handler, String payload) {
        handler.handle(handler.parse(payload));
    }

    private static String eventType(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader(EVENT_TYPE_HEADER);
        return header == null || header.value() == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
