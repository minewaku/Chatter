package com.minewaku.chatter.profilenext.start.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The three-step image change through the real HTTP API, with the storage side played by {@link FakeObjectStore}. */
public abstract class ImageE2eSupport extends StartTestSupport {

    private static final AtomicLong IDS = new AtomicLong(6_000_000L);

    @Autowired
    protected JdbcClient jdbc;

    @BeforeEach
    void resetStorage() {
        STORAGE.reset();
    }

    protected static long newUser() {
        return IDS.incrementAndGet();
    }

    protected HttpResponse<String> requestTicket(long user, String slot, String contentType, long size) throws Exception {
        return send(
                "POST",
                "/api/v1/profiles/@me/images/" + slot + "/upload",
                bearer(user),
                "{\"contentType\":\"" + contentType + "\",\"sizeBytes\":" + size + "}");
    }

    protected HttpResponse<String> commit(long user, String imageId) throws Exception {
        return send("POST", "/api/v1/profiles/@me/images/commit", bearer(user), "{\"imageId\":\"" + imageId + "\"}");
    }

    protected HttpResponse<String> remove(long user, String slot) throws Exception {
        return send("DELETE", "/api/v1/profiles/@me/images/" + slot, bearer(user), null);
    }

    protected String profileOf(long user) throws Exception {
        return send("GET", "/api/v1/profiles/" + user, bearer(user), null).body();
    }

    /** Asks for a ticket for {@code bytes}, then "uploads" them to the address the ticket names. Returns the image id. */
    protected String upload(long user, String slot, String contentType, byte[] bytes) throws Exception {
        HttpResponse<String> ticket = requestTicket(user, slot, contentType, bytes.length);
        assertThat(ticket.statusCode()).as(ticket.body()).isEqualTo(200);
        STORAGE.putViaUrl(string(ticket.body(), "uploadUrl"), bytes, contentType);
        return string(ticket.body(), "imageId");
    }

    /** Upload and commit of a small valid PNG; returns the image id. */
    protected String setImage(long user, String slot, int size) throws Exception {
        String imageId = upload(user, slot, "image/png", ImageFixtures.png(size, size));
        HttpResponse<String> committed = commit(user, imageId);
        assertThat(committed.statusCode()).as(committed.body()).isEqualTo(200);
        return imageId;
    }

    protected static String string(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\":\"([^\"]*)\"").matcher(json);
        assertThat(matcher.find()).as("field " + field + " in " + json).isTrue();
        return matcher.group(1).replace("\\u0026", "&").replace("\\/", "/");
    }

    protected static String keyOf(long user, String slot, String imageId) {
        return "profiles/" + user + "/" + slot + "/" + imageId;
    }

    protected static String addressOf(long user, String slot, String imageId) {
        return "https://media.test/" + keyOf(user, slot, imageId);
    }

    /** The keys the storage holds for this user. */
    protected Set<String> storedKeys(long user) {
        Set<String> keys = new TreeSet<>();
        STORAGE.keys(BUCKET).stream().filter(k -> k.startsWith("profiles/" + user + "/")).forEach(keys::add);
        return keys;
    }

    protected long rows(long user) {
        return jdbc.sql("SELECT count(*) FROM profile WHERE user_id = :id")
                .param("id", user)
                .query(Long.class)
                .single();
    }

    protected long ledgerRows(long user) {
        return jdbc.sql("SELECT count(*) FROM pending_upload WHERE user_id = :id")
                .param("id", user)
                .query(Long.class)
                .single();
    }

    private static String notice(String eventId, long userId) {
        return "{\"eventId\":\"" + eventId + "\",\"userId\":\"" + userId + "\",\"occurredAt\":\"2026-10-29T08:15:30Z\"}";
    }

    protected static String freshEventId() {
        return "evt-" + UUID.randomUUID();
    }

    /** Publishes the way identity-access's relay does: key = user id, headers id and eventType. */
    protected static void publishNotice(long userId, String eventId, int times) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            for (int i = 0; i < times; i++) {
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(ACCOUNT_TOPIC, Long.toString(userId), notice(eventId, userId));
                record.headers().add("id", eventId.getBytes(StandardCharsets.UTF_8));
                record.headers().add("eventType", "UserSoftDeleted".getBytes(StandardCharsets.UTF_8));
                producer.send(record);
            }
            producer.flush();
        }
    }
}
