package com.minewaku.chatter.identityaccess.infrastructure.service.impl;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.HashedPassword;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.Password;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.PasswordHasher;
import com.password4j.Hash;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class Argon2IdPasswordHasher implements PasswordHasher {

    @Override
    public HashedPassword hash(Password rawPassword) {
        Hash hash = com.password4j.Password.hash(rawPassword.getValue())
                .addRandomSalt(16)
                .withArgon2();

        return new HashedPassword(
                "argon2id",
                hash.getResult(),
                hash.getSalt().getBytes());
    }

    @Override
    public boolean matchesRawAndHashedPassword(Password rawPassword, HashedPassword hashedPassword) {
        log.info("imcoming pw: {}, hashed pw: {}", rawPassword.getValue(), hashedPassword.getHash());  
        return com.password4j.Password.check(rawPassword.getValue(), hashedPassword.getHash())
                .withArgon2();
    }
}
