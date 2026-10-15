package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.session;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the single {@code Duration} bean core's {@code AuthenticateService} needs. No other {@code Duration}
 * bean may be registered in this module; other durations are read from properties objects instead.
 */
@Configuration
public class SessionConfiguration {

    @Bean
    Duration sessionLifetime(SessionProperties properties) {
        return properties.lifetime();
    }
}
