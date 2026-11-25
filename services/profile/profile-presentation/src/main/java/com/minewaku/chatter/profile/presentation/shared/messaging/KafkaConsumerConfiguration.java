package com.minewaku.chatter.profile.presentation.shared.messaging;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Wiring shared by every consumer in this service, because the factory below is Spring Kafka's default one: each record
 * is acknowledged once handled (or once dead-lettered), failures are retried in place with exponential backoff so
 * messages for one key stay in order, and a message that still fails, or can never be read, is published to
 * {@code <topic>.profile.dlt} on the same partition.
 */
@Configuration
@EnableConfigurationProperties(ConsumerRetryProperties.class)
public class KafkaConsumerConfiguration {

    static final String DEAD_LETTER_SUFFIX = ".profile.dlt";

    @Bean
    DefaultErrorHandler consumerErrorHandler(KafkaOperations<?, ?> kafka, ConsumerRetryProperties retry) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafka, (record, exception) -> new TopicPartition(record.topic() + DEAD_LETTER_SUFFIX, record.partition()));

        ExponentialBackOff backOff =
                new ExponentialBackOff(retry.initialInterval().toMillis(), retry.multiplier());
        backOff.setMaxInterval(retry.maxInterval().toMillis());
        backOff.setMaxElapsedTime(retry.maxElapsed().toMillis());

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(UnreadableMessageException.class);
        return handler;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory, DefaultErrorHandler consumerErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(consumerErrorHandler);
        return factory;
    }
}
