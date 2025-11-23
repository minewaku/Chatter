package com.minewaku.chatter.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

@Configuration
public class Bucket4jRedisConfig {

    @Bean
    @RefreshScope
    RedisClient redisClient(RedisProperties redisProperties) {
        System.out.println("port: " + redisProperties.getPort());
        System.out.println("host: " + redisProperties.getHost());
        System.out.println("username: " + redisProperties.getUsername());
        System.out.println("password: " + redisProperties.getPassword());
        System.out.println("ssl: " + redisProperties.getSsl());

        RedisURI redisUri = RedisURI.builder()
                .withHost(redisProperties.getHost())
                .withPort(redisProperties.getPort())
                .withAuthentication(redisProperties.getUsername(), redisProperties.getPassword())
                .withTimeout(redisProperties.getTimeout() != null ? redisProperties.getTimeout() : Duration.ofSeconds(20))
                .build();
        return RedisClient.create(redisUri);
    }


    @Bean
    ProxyManager<String> lettuceBasedProxyManager(@Autowired RedisClient redisClient) {
        StatefulRedisConnection<String, byte[]> redisConnection = redisClient
                .connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        return LettuceBasedProxyManager.builderFor(redisConnection)
                .withExpirationStrategy(
                		ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10))
                )
                .build();
    }
}