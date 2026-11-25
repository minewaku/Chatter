package com.minewaku.chatter.profile.infrastructure.support;

import com.minewaku.chatter.profile.infrastructure.adapter.outbound.shared.id.UuidV7IdGenerator;
import com.minewaku.chatter.profile.infrastructure.integration.transaction.TransactionConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcClientAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Minimal context for the JDBC adapters: datasource, JdbcClient, transactions and the adapters themselves. */
@Configuration
@ImportAutoConfiguration({
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    JdbcClientAutoConfiguration.class,
    JacksonAutoConfiguration.class,
    TransactionAutoConfiguration.class
})
@Import({TransactionConfiguration.class, UuidV7IdGenerator.class})
@ComponentScan({
    "com.minewaku.chatter.profile.infrastructure.adapter.outbound.profile",
    "com.minewaku.chatter.profile.infrastructure.adapter.outbound.image.pending"
})
public class JdbcTestConfiguration {}
