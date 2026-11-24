package com.minewaku.chatter.profilenext.infrastructure.integration.image;

import com.minewaku.chatter.profilenext.profile.internal.application.command.ImagePolicy;
import com.minewaku.chatter.profilenext.profile.internal.application.command.SweepPolicy;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Builds the plain-record policies the core's image services are constructed with. */
@Configuration
@EnableConfigurationProperties(ImageProperties.class)
public class ImagePolicyConfiguration {

    @Bean
    ImagePolicy imagePolicy(ImageProperties properties) {
        Set<String> formats = properties.allowedFormats().stream()
                .map(format -> format.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        return new ImagePolicy(
                properties.maxBytes().avatar().toBytes(),
                properties.maxBytes().banner().toBytes(),
                properties.maxDimension().avatar(),
                properties.maxDimension().banner(),
                formats,
                properties.ticketLifetime(),
                properties.grace(),
                Math.toIntExact(properties.inspectBytes().toBytes()));
    }

    @Bean
    SweepPolicy sweepPolicy(ImageProperties properties) {
        return new SweepPolicy(properties.sweep().batchSize(), properties.sweep().retryDelay());
    }
}
