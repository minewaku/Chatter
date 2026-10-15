package com.minewaku.chatter.identityaccess.infrastructure.integration.postgresql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class UnreachableDatabaseStartupTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class))
            .withPropertyValues(
                    "spring.datasource.url=jdbc:postgresql://localhost:1/chatter?currentSchema=identity_access",
                    "spring.datasource.username=admin",
                    "spring.datasource.password=secret-should-not-be-logged",
                    "spring.datasource.hikari.connection-timeout=1000",
                    "spring.flyway.enabled=true",
                    "spring.flyway.schemas=identity_access",
                    "spring.flyway.default-schema=identity_access",
                    "spring.flyway.create-schemas=true");

    @Test
    void startupFailsFastAndTheMessageNamesTheUnreachableStore() {
        runner.run(context -> {
            Throwable failure = context.getStartupFailure();
            assertThat(failure).isNotNull();

            StringBuilder chain = new StringBuilder();
            for (Throwable t = failure; t != null; t = t.getCause()) {
                chain.append(t.getMessage()).append('\n');
            }
            assertThat(chain.toString()).contains("localhost:1");
            assertThat(chain.toString()).doesNotContain("secret-should-not-be-logged");
        });
    }
}
