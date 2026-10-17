package com.minewaku.chatter.identityaccess.presentation.authentication;

import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserAgentConfiguration {

    @Bean
    UserAgentAnalyzer userAgentAnalyzer() {
        return newAnalyzer();
    }

    /** Built once: restricted to the three fields the label uses, which keeps start-up time and memory small. */
    public static UserAgentAnalyzer newAnalyzer() {
        return UserAgentAnalyzer.newBuilder()
                .hideMatcherLoadStats()
                .withCache(1_000)
                .withField("AgentName")
                .withField("OperatingSystemName")
                .withField("DeviceClass")
                .immediateInitialization()
                .build();
    }
}
