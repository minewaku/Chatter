package com.minewaku.chatter.identityaccess.user.internal.model;

import java.util.Arrays;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
public final class PasswordHash {

    @Getter
    private final String algorithm;

    @Getter
    private final String hash;

    private final byte[] salt;

    public PasswordHash(String algorithm, String hash, byte[] salt) {
        if (algorithm == null
                || algorithm.isBlank()
                || hash == null
                || hash.isBlank()
                || salt == null
                || salt.length == 0) {
            throw new IllegalArgumentException("Password hash requires algorithm, hash, and salt");
        }
        this.algorithm = algorithm;
        this.hash = hash;
        this.salt = Arrays.copyOf(salt, salt.length);
    }

    public byte[] salt() {
        return Arrays.copyOf(salt, salt.length);
    }
}
