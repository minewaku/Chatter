package com.minewaku.chatter.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

import com.minewaku.chatter.identityaccess.infrastructure.support.RsaKeyFixtures;
import java.security.KeyPair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** With no configured origins (the default) cross-origin browser calls are denied. */
@SpringJUnitWebConfig(SecurityChainConfig.class)
class SecurityChainDefaultCorsTest {

    static final KeyPair PAIR = RsaKeyFixtures.newKeyPair();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("identity-access.security.jwt.private-key", () -> RsaKeyFixtures.privatePem(PAIR));
        registry.add("identity-access.security.jwt.public-key", () -> RsaKeyFixtures.publicPem(PAIR));
    }

    @Autowired
    WebApplicationContext context;

    @Test
    void noOriginIsAllowedByDefault() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        MvcResult result = mvc.perform(options("/probe/protected")
                        .header("Origin", "https://app.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andReturn();

        assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin"))
                .isNull();
    }
}
