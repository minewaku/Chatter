package com.minewaku.chatter.identityaccess.infrastructure.support;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Starts one PostgreSQL 17 container shared by the test class hierarchy, migrates the identity-access
 * schema with the production Flyway settings, and exposes the connection settings used in development.
 */
public abstract class PostgreSqlTestSupport {

    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("chatter")
            .withUsername("admin")
            .withPassword("123456");

    static {
        POSTGRES.start();
    }

    public static String jdbcUrl() {
        return "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432)
                + "/chatter?currentSchema=identity_access";
    }

    public static String username() {
        return POSTGRES.getUsername();
    }

    public static String password() {
        return POSTGRES.getPassword();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgreSqlTestSupport::jdbcUrl);
        registry.add("spring.datasource.username", PostgreSqlTestSupport::username);
        registry.add("spring.datasource.password", PostgreSqlTestSupport::password);
    }

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(jdbcUrl(), username(), password())
                .schemas("identity_access")
                .defaultSchema("identity_access")
                .createSchemas(true)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}
