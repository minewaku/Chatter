package com.minewaku.chatter.identityaccess.user.internal.model;

import java.util.Arrays;
import java.util.Objects;

public final class PasswordHash {

    private final String algorithm;
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

    public String algorithm() {
        return algorithm;
    }

    public String hash() {
        return hash;
    }

    public byte[] salt() {
        return Arrays.copyOf(salt, salt.length);
    }

    @Override
    public boolean equals(Object candidate) {
        return candidate instanceof PasswordHash other
                && algorithm.equals(other.algorithm)
                && hash.equals(other.hash)
                && Arrays.equals(salt, other.salt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(algorithm, hash, Arrays.hashCode(salt));
    }
}
