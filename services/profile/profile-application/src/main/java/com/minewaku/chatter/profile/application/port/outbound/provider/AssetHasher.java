package com.minewaku.chatter.profile.application.port.outbound.provider;

import java.io.InputStream;

public interface AssetHasher {
    String hash(InputStream inputStream);
} 
