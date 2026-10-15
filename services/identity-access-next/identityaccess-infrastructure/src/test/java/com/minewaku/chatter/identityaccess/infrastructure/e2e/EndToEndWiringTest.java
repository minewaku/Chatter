package com.minewaku.chatter.identityaccess.infrastructure.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minewaku.chatter.identityaccess.infrastructure.structure.StructureRules;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** The whole service starts against real PostgreSQL and Redis and every core port has exactly one adapter bean. */
@SpringBootTest(classes = E2eTestApplication.class)
class EndToEndWiringTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        E2eProperties.values().forEach((key, value) -> registry.add(key, () -> value));
    }

    @Autowired
    WebApplicationContext context;

    @Autowired
    JdbcClient jdbc;

    @Test
    void everyCorePortResolvesToExactlyOneBean() throws ClassNotFoundException {
        for (StructureRules.PortRow row : StructureRules.PORT_MAP) {
            Class<?> port = Class.forName(row.port());

            assertThat(context.getBeansOfType(port)).as(row.port()).hasSize(1);
        }
    }

    @Test
    void migrationsRanInTheServiceSchemaOnly() {
        Integer history = jdbc.sql("select count(*) from identity_access.flyway_schema_history")
                .query(Integer.class)
                .single();
        String publicTable = jdbc.sql("select to_regclass('public.user_account')::text")
                .query(String.class)
                .optional()
                .orElse(null);

        assertThat(history).isGreaterThanOrEqualTo(1);
        assertThat(publicTable).isNull();
    }

    @Test
    void healthIsUpWithBothStoresRunning() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
