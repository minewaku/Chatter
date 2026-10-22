package com.minewaku.chatter.identityaccess.infrastructure.security;

import com.minewaku.chatter.identityaccess.infrastructure.support.RsaKeyFixtures;
import java.security.KeyPair;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/** Web context for the chain tests: the real security configuration plus the probe controller. */
@Configuration
@EnableWebMvc
@EnableConfigurationProperties({JwtProperties.class, SecurityProperties.class})
@Import({SecurityConfiguration.class, JwtDecoderConfiguration.class})
class SecurityChainConfig {

    /** The pair the chain tests sign with and the decoder verifies with. */
    static final KeyPair PAIR = RsaKeyFixtures.newKeyPair();

    @Bean
    PemKeys.RsaKeyPair signingKeys() {
        return RsaKeyFixtures.keys(PAIR);
    }

    @Bean
    SecurityProbeController probeController() {
        return new SecurityProbeController();
    }
}
