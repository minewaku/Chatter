package com.minewaku.chatter.profilenext.presentation.account.messaging;

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
 * Consumer wiring: each record is acknowledged once handled (or once dead-lettered), failures are retried in place
 * with exponential backoff so notices for one user stay in order, and a notice that still fails, or can never be read,
 * is published to {@code <topic>.profile-next.dlt} on the same partition.
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class AccountDeletedConsumerConfiguration {

    static final String DEAD_LETTER_SUFFIX = ".profile-next.dlt";

    @Bean
    DefaultErrorHandler accountDeletedErrorHandler(KafkaOperations<?, ?> kafka, MessagingProperties properties) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafka, (record, exception) -> new TopicPartition(record.topic() + DEAD_LETTER_SUFFIX, record.partition()));

        MessagingProperties.Retry retry = properties.retry();
        ExponentialBackOff backOff =
                new ExponentialBackOff(retry.initialInterval().toMillis(), retry.multiplier());
        backOff.setMaxInterval(retry.maxInterval().toMillis());
        backOff.setMaxElapsedTime(retry.maxElapsed().toMillis());

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(UnreadableNoticeException.class);
        return handler;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory, DefaultErrorHandler accountDeletedErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(accountDeletedErrorHandler);
        return factory;
    }
}
