package com.minewaku.chatter.profilenext.infrastructure.integration.postgresql;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database wiring for the JDBC adapters. The adapters use {@code JdbcClient} and hand-written statements, so there
 * are no repositories to scan; the datasource, {@code JdbcClient} and the transaction manager come from Boot's
 * auto-configuration, and this class only makes sure {@code @Transactional} on core services is honoured.
 */
@Configuration
@EnableTransactionManagement
public class PostgreSqlConfiguration {}
