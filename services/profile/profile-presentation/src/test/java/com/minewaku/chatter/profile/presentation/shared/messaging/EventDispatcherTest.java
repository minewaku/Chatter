package com.minewaku.chatter.profile.presentation.shared.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

class EventDispatcherTest {

    /** Handles one type and remembers what reached it. */
    static class RecordingHandler implements EventHandler<String> {
        private final String type;
        final List<String> handled = new ArrayList<>();

        RecordingHandler(String type) {
            this.type = type;
        }

        @Override
        public String eventType() {
            return type;
        }

        @Override
        public String parse(String payload) {
            if (payload.isBlank()) {
                throw new UnreadableMessageException("blank");
            }
            return payload.toUpperCase();
        }

        @Override
        public void handle(String message) {
            handled.add(message);
        }
    }

    private static ConsumerRecord<String, String> record(String eventType, String value) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, "key", value);
        if (eventType != null) {
            record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
        }
        return record;
    }

    @Test
    void aRecordGoesToTheHandlerOfItsTypeAndOnlyThatOne() {
        RecordingHandler created = new RecordingHandler("Created");
        RecordingHandler deleted = new RecordingHandler("Deleted");
        EventDispatcher dispatcher = new EventDispatcher(List.of(created, deleted));

        dispatcher.dispatch(record("Deleted", "x"));

        assertThat(deleted.handled).containsExactly("X");
        assertThat(created.handled).isEmpty();
    }

    @Test
    void aTypeNobodyHandlesIsSkipped() {
        RecordingHandler created = new RecordingHandler("Created");
        EventDispatcher dispatcher = new EventDispatcher(List.of(created));

        dispatcher.dispatch(record("SomethingElse", "x"));

        assertThat(created.handled).isEmpty();
    }

    @Test
    void aRecordWithNoTypeIsUnreadable() {
        EventDispatcher dispatcher = new EventDispatcher(List.of(new RecordingHandler("Created")));

        assertThatThrownBy(() -> dispatcher.dispatch(record(null, "x"))).isInstanceOf(UnreadableMessageException.class);
    }

    @Test
    void aPayloadTheHandlerCannotReadSurfacesAsUnreadable() {
        EventDispatcher dispatcher = new EventDispatcher(List.of(new RecordingHandler("Created")));

        assertThatThrownBy(() -> dispatcher.dispatch(record("Created", " ")))
                .isInstanceOf(UnreadableMessageException.class);
    }

    @Test
    void twoHandlersForOneTypeStopTheApplicationFromStarting() {
        List<EventHandler<?>> handlers = List.of(new RecordingHandler("Created"), new RecordingHandler("Created"));

        assertThatThrownBy(() -> new EventDispatcher(handlers))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Created");
    }
}
