package com.minewaku.chatter.profile.infrastructure.cache.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minewaku.chatter.profile.application.port.outbound.query.model.ProfileReadModel;



@Configuration
public class RedisConfig {

    @Bean
    LettuceConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                redisProperties.getHost(),
                redisProperties.getPort());

        config.setUsername(redisProperties.getUsername());
        config.setPassword(redisProperties.getPassword());

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        return factory;
    }

    @Bean
    GenericJackson2JsonRedisSerializer customJsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    @Bean
    RedisTemplate<String, ProfileReadModel> redisTemplateProfileReadModel(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, ProfileReadModel> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<ProfileReadModel> serializer = new Jackson2JsonRedisSerializer<>(objectMapper,
                ProfileReadModel.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer) {
        // CacheManager thường hoạt động độc lập, nhưng nếu muốn nó sync với transaction
        // thì cấu hình ở level transaction manager, tuy nhiên code này giữ nguyên là ổn.
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(genericJackson2JsonRedisSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .transactionAware() // Thêm dòng này nếu muốn Cache cũng rollback khi lỗi
                .build();
    }
}