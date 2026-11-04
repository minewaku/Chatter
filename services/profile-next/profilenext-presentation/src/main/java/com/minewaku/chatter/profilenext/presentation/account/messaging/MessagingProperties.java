package com.minewaku.chatter.profilenext.presentation.account.messaging;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Topic and retry settings for the account-deleted consumer. */
@ConfigurationProperties("profile-next.messaging")
public record MessagingProperties(String accountTopic, Retry retry) {

    public MessagingProperties {
        if (accountTopic == null || accountTopic.isBlank()) {
            accountTopic = "identityaccess.user.v1";
        }
        if (retry == null) {
            retry = new Retry(null, null, null, null);
        }
    }

    /** Blocking retries with exponential backoff before a notice is sent to the dead-letter topic. */
    public record Retry(Duration initialInterval, Double multiplier, Duration maxInterval, Duration maxElapsed) {

        public Retry {
            initialInterval = initialInterval == null ? Duration.ofSeconds(1) : initialInterval;
            multiplier = multiplier == null ? 2.0 : multiplier;
            maxInterval = maxInterval == null ? Duration.ofSeconds(30) : maxInterval;
            maxElapsed = maxElapsed == null ? Duration.ofMinutes(2) : maxElapsed;
        }
    }
}
