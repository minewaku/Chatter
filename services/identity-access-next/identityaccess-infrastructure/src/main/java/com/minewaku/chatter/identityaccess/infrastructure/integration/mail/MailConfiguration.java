package com.minewaku.chatter.identityaccess.infrastructure.integration.mail;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/** Enables Spring's built-in {@code @Retryable} for the mail adapter; no extra retry library is used. */
@Configuration
@EnableResilientMethods(proxyTargetClass = true)
public class MailConfiguration {}
