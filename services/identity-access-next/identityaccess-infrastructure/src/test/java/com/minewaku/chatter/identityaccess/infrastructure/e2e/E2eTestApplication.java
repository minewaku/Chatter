package com.minewaku.chatter.identityaccess.infrastructure.e2e;

import java.time.Clock;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * The service as the start module wires it (core plus every infrastructure package), minus the test-only classes
 * that share those packages and with a recording mail sender in place of SMTP.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ConfigurationPropertiesScan("com.minewaku.chatter.identityaccess")
@ComponentScan(
        basePackages = {
            "com.minewaku.chatter.identityaccess.user",
            "com.minewaku.chatter.identityaccess.authentication",
            "com.minewaku.chatter.identityaccess.emailverification",
            "com.minewaku.chatter.identityaccess.shared",
            "com.minewaku.chatter.identityaccess.infrastructure.adapter",
            "com.minewaku.chatter.identityaccess.infrastructure.integration",
            "com.minewaku.chatter.identityaccess.infrastructure.security"
        },
        excludeFilters = {
            @ComponentScan.Filter(
                    type = FilterType.CUSTOM,
                    classes = {TypeExcludeFilter.class, AutoConfigurationExcludeFilter.class}),
            @ComponentScan.Filter(
                    type = FilterType.REGEX,
                    pattern = ".*(Test|Test\\$.*|SecurityChainConfig|SecurityProbeController)")
        })
public class E2eTestApplication {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RecordingMailSender mailSender() {
        return new RecordingMailSender();
    }
}
