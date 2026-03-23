package com.minewaku.chatter.identityaccess.infrastructure.service.impl;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.identityaccess.application.port.outbound.provider.PasswordHasher;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.HashedPassword;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.Password;
import com.password4j.Hash;

@Service
public class Argon2IdPasswordHasher implements PasswordHasher {

    @Override
    public HashedPassword hash(Password rawPassword) {
        Hash hash = com.password4j.Password.hash(rawPassword.getValue())
                .addRandomSalt(16)
                .withArgon2();

        System.out.println("Generated salt (base64): " + hash.toString());

        return new HashedPassword(
                "argon2id",
                hash.getResult(),
                hash.getSalt().getBytes());
    }

    @Override
    public boolean matchesRawAndHashedPassword(Password rawPassword, HashedPassword hashedPassword) {
        return com.password4j.Password.check(rawPassword.getValue(), hashedPassword.getHash())
                .withArgon2();
    }
}
