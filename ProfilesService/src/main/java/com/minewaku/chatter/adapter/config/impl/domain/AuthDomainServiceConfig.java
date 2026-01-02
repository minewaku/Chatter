package com.minewaku.chatter.adapter.config.impl.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.minewaku.chatter.domain.port.out.service.PasswordHasher;
import com.minewaku.chatter.domain.service.auth.CheckRegisterUserDomainService;
import com.minewaku.chatter.domain.service.auth.PasswordSecurityDomainService;

@Configuration
public class AuthDomainServiceConfig {

    @Bean
    PasswordSecurityDomainService passwordSecurityDomainService(
            PasswordHasher passwordHasher) {
        return new PasswordSecurityDomainService(passwordHasher);
    }

    @Bean
    CheckRegisterUserDomainService checkRegisterUserDomainService() {
        return new CheckRegisterUserDomainService();
    }
}
