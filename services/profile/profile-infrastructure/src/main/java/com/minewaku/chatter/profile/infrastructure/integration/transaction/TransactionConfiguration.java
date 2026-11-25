package com.minewaku.chatter.profile.infrastructure.integration.transaction;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Makes sure {@code @Transactional} on core services is honoured. The JDBC adapters use {@code JdbcClient} and
 * hand-written statements, so there are no repositories to scan; the datasource, {@code JdbcClient} and the
 * transaction manager come from Boot's auto-configuration.
 */
@Configuration
@EnableTransactionManagement
public class TransactionConfiguration {}
