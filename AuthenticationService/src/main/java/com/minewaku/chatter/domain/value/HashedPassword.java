package com.minewaku.chatter.domain.value;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class HashedPassword {
	
	@NonNull
    private final String algorithm;
	
	@NonNull
    private final String hash;
	
	@NonNull
    private final byte[] salt;

	@JsonCreator
    public HashedPassword(
    		@JsonProperty("algorithm") @NonNull String algorithm, 
    		@JsonProperty("hash") @NonNull String hash, 
    		@JsonProperty("salt") @NonNull byte[] salt) {
        
        if (algorithm.isBlank()) {
            throw new IllegalArgumentException("Algorithm cannot be blank");
        }

        if (hash.isBlank()) {
            throw new IllegalArgumentException("Hash cannot be blank");
        }

        if (salt.length == 0) {
            throw new IllegalArgumentException("Salt cannot be empty");
        }

        this.algorithm = algorithm.toLowerCase();
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

}
