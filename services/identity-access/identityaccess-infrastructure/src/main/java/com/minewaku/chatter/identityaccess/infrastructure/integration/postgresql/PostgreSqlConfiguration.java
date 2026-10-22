package com.minewaku.chatter.identityaccess.infrastructure.integration.postgresql;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * Spring Data JDBC setup for the write-side account rows. The explicit {@code @EnableJdbcRepositories} restricts
 * repository scanning to the adapter package and replaces Boot's auto-configuration, so the base JDBC
 * configuration (aggregate template, mapping context, converters) is inherited here.
 */
@Configuration
@EnableJdbcRepositories(
        basePackages = "com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.write.jdbc")
public class PostgreSqlConfiguration extends AbstractJdbcConfiguration {}
