package com.minewaku.chatter.domain.value.file;

import java.io.InputStream;

import com.minewaku.chatter.domain.value.id.StorageKey;

public interface StorableFile {
    InputStream getInputStream();
    StorageKey getStorageKey();
    String getPath();
}
