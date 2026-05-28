package com.minewaku.chatter.profile.presentation.messaging.config;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean(name = "singleFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> singleFactory(
            KafkaProperties kafkaProperties) {
        
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10); 
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 600000); 

        ConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(props);
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(false);
        return factory;
    }

    @Bean(name = "batchFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> batchFactory(
            KafkaProperties kafkaProperties) {
        
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 60000); 
        
        ConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(props);
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        return factory;
    }
}