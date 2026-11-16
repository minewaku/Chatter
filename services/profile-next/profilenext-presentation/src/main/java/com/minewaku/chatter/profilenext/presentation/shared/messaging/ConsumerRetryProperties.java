package com.minewaku.chatter.profilenext.presentation.shared.messaging;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Blocking retries with exponential backoff before a message is sent to the dead-letter topic. Shared by every consumer. */
@ConfigurationProperties("profile-next.messaging.retry")
public record ConsumerRetryProperties(
        Duration initialInterval, Double multiplier, Duration maxInterval, Duration maxElapsed) {

    public ConsumerRetryProperties {
        initialInterval = initialInterval == null ? Duration.ofSeconds(1) : initialInterval;
        multiplier = multiplier == null ? 2.0 : multiplier;
        maxInterval = maxInterval == null ? Duration.ofSeconds(30) : maxInterval;
        maxElapsed = maxElapsed == null ? Duration.ofMinutes(2) : maxElapsed;
    }
}
