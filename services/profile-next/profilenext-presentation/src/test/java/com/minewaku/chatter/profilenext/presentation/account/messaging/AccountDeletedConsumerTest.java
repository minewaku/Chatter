package com.minewaku.chatter.profilenext.presentation.account.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.minewaku.chatter.profilenext.presentation.shared.messaging.EventDispatcher;
import com.minewaku.chatter.profilenext.presentation.shared.messaging.KafkaConsumerConfiguration;
import com.minewaku.chatter.profilenext.profile.api.command.DeleteProfileUseCase;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.ConfluentKafkaContainer;

@SpringBootTest(
        classes = AccountDeletedConsumerTest.TestApplication.class,
        properties = {
            "spring.kafka.consumer.group-id=profile-next",
            "spring.kafka.consumer.auto-offset-reset=earliest",
            "spring.kafka.consumer.enable-auto-commit=false",
            "profile-next.messaging.account-topic=identityaccess.user.v1",
            "profile-next.messaging.retry.initial-interval=PT0.05S",
            "profile-next.messaging.retry.multiplier=2",
            "profile-next.messaging.retry.max-interval=PT0.2S",
            "profile-next.messaging.retry.max-elapsed=PT1S"
        })
class AccountDeletedConsumerTest {

    private static final String TOPIC = "identityaccess.user.v1";
    private static final String DLT = "identityaccess.user.v1.profile-next.dlt";

    private static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.6.0");

    static {
        KAFKA.start();
        Properties admin = new Properties();
        admin.put("bootstrap.servers", KAFKA.getBootstrapServers());
        try (AdminClient client = AdminClient.create(admin)) {
            client.createTopics(List.of(new NewTopic(TOPIC, 3, (short) 1), new NewTopic(DLT, 3, (short) 1)))
                    .all()
                    .get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void kafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    /** Records every call and can be told to fail for a user. */
    static class RecordingUseCase implements DeleteProfileUseCase {
        final List<Command> calls = Collections.synchronizedList(new ArrayList<>());
        final Map<Long, Integer> failuresLeft = new ConcurrentHashMap<>();
        final Map<Long, Boolean> alwaysFail = new ConcurrentHashMap<>();

        @Override
        public Result handle(Command command) {
            calls.add(command);
            if (alwaysFail.containsKey(command.userId())) {
                throw new IllegalStateException("database is down");
            }
            Integer left = failuresLeft.get(command.userId());
            if (left != null && left > 0) {
                failuresLeft.put(command.userId(), left - 1);
                throw new IllegalStateException("temporary failure");
            }
            return new Result();
        }

        List<Command> callsFor(long userId) {
            synchronized (calls) {
                return calls.stream().filter(c -> c.userId() == userId).toList();
            }
        }
    }

    @Configuration
    @ImportAutoConfiguration(KafkaAutoConfiguration.class)
    @Import({
        AccountTopicListener.class,
        AccountDeletedHandler.class,
        EventDispatcher.class,
        KafkaConsumerConfiguration.class
    })
    static class TestApplication {

        @Bean
        RecordingUseCase recordingUseCase() {
            return new RecordingUseCase();
        }
    }

    @Autowired
    private RecordingUseCase useCase;

    private static final AtomicUser USERS = new AtomicUser();

    static class AtomicUser {
        private long next = 70_000L;

        synchronized long next() {
            return ++next;
        }
    }

    private static String notice(String eventId, long userId) {
        return "{\"eventId\":\"" + eventId + "\",\"userId\":\"" + userId + "\",\"occurredAt\":\"2026-10-29T08:15:30Z\"}";
    }

    private static void publish(String key, String value, String eventType) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, key, value);
            record.headers().add("id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            if (eventType != null) {
                record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
            }
            producer.send(record).get();
        }
    }

    private static List<ConsumerRecord<String, String>> deadLetters() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-reader-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        List<ConsumerRecord<String, String>> found = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(DLT));
            for (int i = 0; i < 6; i++) {
                consumer.poll(Duration.ofMillis(500)).forEach(found::add);
            }
        }
        return found;
    }

    private static boolean deadLettered(long userId) {
        return deadLetters().stream().anyMatch(record -> Long.toString(userId).equals(record.key()));
    }

    @Test
    void aUserSoftDeletedRecordIsDeliveredOnceWithTheParsedIds() throws Exception {
        long user = USERS.next();

        publish(Long.toString(user), notice("evt-1", user), "UserSoftDeleted");

        await().atMost(Duration.ofSeconds(30)).until(() -> useCase.callsFor(user).size() == 1);
        var call = useCase.callsFor(user).get(0);
        assertThat(call.eventId()).isEqualTo("evt-1");
        assertThat(call.userId()).isEqualTo(user);
        assertThat(call.occurredAt().toString()).isEqualTo("2026-10-29T08:15:30Z");
    }

    @Test
    void anotherEventTypeIsSkippedWithoutACallOrADeadLetter() throws Exception {
        long skipped = USERS.next();
        long marker = USERS.next();

        publish(Long.toString(skipped), notice("evt-2", skipped), "SomethingElse");
        publish(Long.toString(marker), notice("evt-3", marker), "UserSoftDeleted");

        await().atMost(Duration.ofSeconds(30)).until(() -> useCase.callsFor(marker).size() == 1);
        assertThat(useCase.callsFor(skipped)).isEmpty();
        assertThat(deadLettered(skipped)).isFalse();
    }

    @Test
    void aRecordWithNoEventTypeGoesToTheDeadLetterTopicAndALaterNoticeIsStillProcessed() throws Exception {
        long untyped = USERS.next();
        long good = USERS.next();

        publish(Long.toString(untyped), notice("evt-9", untyped), null);
        publish(Long.toString(good), notice("evt-10", good), "UserSoftDeleted");

        await().atMost(Duration.ofSeconds(30)).until(() -> useCase.callsFor(good).size() == 1);
        await().atMost(Duration.ofSeconds(30)).until(() -> deadLettered(untyped));
        assertThat(useCase.callsFor(untyped)).isEmpty();
    }

    @Test
    void invalidJsonGoesToTheDeadLetterTopicAtOnceAndALaterNoticeIsStillProcessed() throws Exception {
        long bad = USERS.next();
        long good = USERS.next();

        publish(Long.toString(bad), "this is not json", "UserSoftDeleted");
        publish(Long.toString(good), notice("evt-4", good), "UserSoftDeleted");

        await().atMost(Duration.ofSeconds(30)).until(() -> useCase.callsFor(good).size() == 1);
        await().atMost(Duration.ofSeconds(30)).until(() -> deadLettered(bad));
        assertThat(useCase.callsFor(bad)).isEmpty();
    }

    @Test
    void aUseCaseThatFailsTwiceThenSucceedsIsRetriedAndProcessed() throws Exception {
        long user = USERS.next();
        useCase.failuresLeft.put(user, 2);

        publish(Long.toString(user), notice("evt-5", user), "UserSoftDeleted");

        await().atMost(Duration.ofSeconds(30)).until(() -> useCase.callsFor(user).size() == 3);
        assertThat(deadLettered(user)).isFalse();
    }

    @Test
    void aUseCaseThatAlwaysFailsIsRetriedThenDeadLetteredAndTheNextRecordIsProcessed() throws Exception {
        long failing = USERS.next();
        long next = USERS.next();
        useCase.alwaysFail.put(failing, true);

        publish(Long.toString(failing), notice("evt-6", failing), "UserSoftDeleted");
        publish(Long.toString(next), notice("evt-7", next), "UserSoftDeleted");

        await().atMost(Duration.ofSeconds(60)).until(() -> useCase.callsFor(next).size() == 1);
        await().atMost(Duration.ofSeconds(30)).until(() -> deadLettered(failing));
        // the retries finish before the dead letter is written, so by now it was tried more than twice
        assertThat(useCase.callsFor(failing).size()).isGreaterThan(2);
    }

    @Test
    void aThousandDeliveriesOfOneNoticeAllReachTheUseCaseWhichIsIdempotent() throws Exception {
        long user = USERS.next();
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            for (int i = 0; i < 1000; i++) {
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(TOPIC, Long.toString(user), notice("evt-8", user));
                record.headers().add("eventType", "UserSoftDeleted".getBytes(StandardCharsets.UTF_8));
                producer.send(record);
            }
            producer.flush();
        }

        await().atMost(Duration.ofSeconds(60)).until(() -> useCase.callsFor(user).size() == 1000);
        assertThat(useCase.callsFor(user)).allMatch(call -> call.eventId().equals("evt-8"));
    }

    @Test
    void recordsForOneUserAreHandledInOrder() throws Exception {
        long user = USERS.next();

        publish(Long.toString(user), notice("first", user), "UserSoftDeleted");
        publish(Long.toString(user), notice("second", user), "UserSoftDeleted");

        await().atMost(Duration.ofSeconds(30)).until(() -> useCase.callsFor(user).size() == 2);
        assertThat(useCase.callsFor(user)).extracting(DeleteProfileUseCase.Command::eventId)
                .containsExactly("first", "second");
    }
}
