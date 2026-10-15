package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.authentication;

import com.minewaku.chatter.identityaccess.authentication.internal.port.AccessTokenIssuer;
import com.minewaku.chatter.identityaccess.infrastructure.security.JwtProperties;
import com.minewaku.chatter.identityaccess.infrastructure.security.PemKeys;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtAccessTokenIssuer(JwtProperties properties, Clock clock) {
        PemKeys.RsaKeyPair keys = PemKeys.load(properties);
        RSAKey jwk = new RSAKey.Builder(keys.publicKey())
                .privateKey(keys.privateKey())
                .build();
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public String issue(UserId userId, Email email) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .audience(List.of(properties.audience()))
                .subject(String.valueOf(userId.value()))
                .claim("email", email.value())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
