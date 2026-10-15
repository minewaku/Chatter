package com.minewaku.chatter.identityaccess.infrastructure.support;

import com.minewaku.chatter.identityaccess.infrastructure.integration.postgresql.PostgreSqlConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcClientAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Minimal context for the user JDBC adapters: datasource, JdbcClient, Spring Data JDBC and the adapters. */
@Configuration
@ImportAutoConfiguration({
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    JdbcClientAutoConfiguration.class
})
@Import(PostgreSqlConfiguration.class)
@ComponentScan("com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user")
public class JdbcTestConfiguration {}
