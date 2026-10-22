package com.minewaku.chatter.identityaccess.infrastructure.integration.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The circuit breakers, built in code from the plain library (no Spring starter). {@code mailBreaker} guards mail
 * delivery; {@code sessionStoreBreaker} guards the Redis calls on the request path. Adds no {@code Duration} bean.
 */
@Configuration
public class ResilienceConfiguration {

    public static final String MAIL = "mailBreaker";
    public static final String SESSION_STORE = "sessionStoreBreaker";

    private static final Logger log = LoggerFactory.getLogger(ResilienceConfiguration.class);

    @Bean
    CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean(MAIL)
    CircuitBreaker mailBreaker(CircuitBreakerRegistry registry, ResilienceProperties properties) {
        return breaker(registry, MAIL, properties.mail());
    }

    @Bean(SESSION_STORE)
    CircuitBreaker sessionStoreBreaker(CircuitBreakerRegistry registry, ResilienceProperties properties) {
        return breaker(registry, SESSION_STORE, properties.sessionStore());
    }

    private static CircuitBreaker breaker(
            CircuitBreakerRegistry registry, String name, ResilienceProperties.Breaker settings) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(settings.windowSize())
                .minimumNumberOfCalls(settings.minimumCalls())
                .failureRateThreshold(settings.failureRatePercent())
                .waitDurationInOpenState(settings.openDuration())
                .permittedNumberOfCallsInHalfOpenState(settings.trialCalls())
                .build();
        CircuitBreaker breaker = registry.circuitBreaker(name, config);
        breaker.getEventPublisher()
                .onStateTransition(event -> log.warn(
                        "breaker={} from={} to={}",
                        name,
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()));
        return breaker;
    }
}
