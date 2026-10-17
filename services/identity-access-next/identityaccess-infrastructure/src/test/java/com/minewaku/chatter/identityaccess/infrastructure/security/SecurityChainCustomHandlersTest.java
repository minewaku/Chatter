package com.minewaku.chatter.identityaccess.infrastructure.security;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minewaku.chatter.identityaccess.infrastructure.support.RsaKeyFixtures;
import java.security.KeyPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** The presentation module supplies its own entry point bean; the chain must use it instead of the default. */
@SpringJUnitWebConfig(SecurityChainCustomHandlersTest.Config.class)
class SecurityChainCustomHandlersTest {

    static final KeyPair PAIR = RsaKeyFixtures.newKeyPair();

    @Configuration
    @Import(SecurityChainConfig.class)
    static class Config {

        @Bean
        AuthenticationEntryPoint customEntryPoint() {
            return (request, response, exception) -> {
                response.setStatus(401);
                response.getWriter().write("custom-entry-point");
            };
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("identity-access.security.jwt.private-key", () -> RsaKeyFixtures.privatePem(PAIR));
        registry.add("identity-access.security.jwt.public-key", () -> RsaKeyFixtures.publicPem(PAIR));
    }

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void missingCredentialIsAnsweredByTheSuppliedEntryPoint() throws Exception {
        mvc.perform(get("/probe/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("custom-entry-point"));
    }

    @Test
    void invalidCredentialIsAnsweredByTheSuppliedEntryPoint() throws Exception {
        mvc.perform(get("/probe/protected").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("custom-entry-point"));
    }
}
