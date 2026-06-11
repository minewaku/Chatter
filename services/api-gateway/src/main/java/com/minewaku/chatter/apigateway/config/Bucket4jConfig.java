package com.minewaku.chatter.apigateway.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class Bucket4jConfig {

    @Bean
    @RefreshScope
    RedisClient redisClient(RedisProperties redisProperties) {
        log.info("port: " + redisProperties.getPort());
        log.info("host: " + redisProperties.getHost());
        log.info("username: " + redisProperties.getUsername());
        log.info("password: " + redisProperties.getPassword());
        log.info("ssl: " + redisProperties.getSsl().isEnabled());

        RedisURI redisUri = RedisURI.builder()
                .withHost(redisProperties.getHost())
                .withPort(redisProperties.getPort())
                .withAuthentication(redisProperties.getUsername(), redisProperties.getPassword())
                .withTimeout(redisProperties.getTimeout() != null ? redisProperties.getTimeout() : Duration.ofSeconds(20))
                .withSsl(redisProperties.getSsl().isEnabled())
                .build();
        return RedisClient.create(redisUri);
    }

    @Bean
    ProxyManager<String> lettuceBasedProxyManager(@Autowired RedisClient redisClient) {
        StatefulRedisConnection<String, byte[]> redisConnection = redisClient
                .connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        ClientSideConfig clientSideConfig = ClientSideConfig.getDefault()
                .withExpirationAfterWriteStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10))
                );

        return LettuceBasedProxyManager.builderFor(redisConnection)
                .withClientSideConfig(clientSideConfig)
                .build();
    }
}