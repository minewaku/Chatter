package com.minewaku.chatter.domain.model;

import java.net.URI;

import com.minewaku.chatter.domain.value.id.StorageKey;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class StorageFile {
    private final StorageKey key;
    private final URI uri;

    public StorageFile(StorageKey key, URI uri) {
        if (key == null || uri == null) {
            throw new IllegalArgumentException("Key and uri must not be null");
        }
        this.key = key;
        this.uri = uri;
    }
}
