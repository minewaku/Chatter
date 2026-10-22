package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.password;

import com.minewaku.chatter.identityaccess.user.internal.model.PasswordHash;
import com.minewaku.chatter.identityaccess.user.internal.model.PlainPassword;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import com.password4j.Argon2Function;
import com.password4j.Hash;
import com.password4j.Password;
import com.password4j.types.Argon2;
import java.nio.CharBuffer;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class Argon2PasswordHasher implements PasswordHasher {

    static final String ALGORITHM = "argon2id";
    private static final int SALT_BYTES = 16;
    private static final Argon2Function FUNCTION = Argon2Function.getInstance(65_536, 3, 1, 32, Argon2.ID);

    private final SecureRandom random = new SecureRandom();

    @Override
    public PasswordHash hash(PlainPassword password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        Hash hash = Password.hash(CharBuffer.wrap(password.characters()))
                .addSalt(salt)
                .with(FUNCTION);
        return new PasswordHash(ALGORITHM, hash.getResult(), hash.getSaltBytes());
    }

    @Override
    public boolean matches(PlainPassword password, PasswordHash passwordHash) {
        if (!ALGORITHM.equals(passwordHash.algorithm())) {
            return false;
        }
        try {
            Argon2Function function = Argon2Function.getInstanceFromHash(passwordHash.hash());
            return Password.check(CharBuffer.wrap(password.characters()), passwordHash.hash())
                    .addSalt(passwordHash.salt())
                    .with(function);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
