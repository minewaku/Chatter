package com.minewaku.chatter.identityaccess.presentation.web.shared;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nl.basjes.parse.useragent.UserAgentAnalyzer;

@Configuration 
public class YauaaConfiguration {
    
    @Bean
    public UserAgentAnalyzer userAgentAnalyzer() {
        return UserAgentAnalyzer
                .newBuilder()
                .hideMatcherLoadStats()
                .withCache(10000)
                .build();
    }
}
