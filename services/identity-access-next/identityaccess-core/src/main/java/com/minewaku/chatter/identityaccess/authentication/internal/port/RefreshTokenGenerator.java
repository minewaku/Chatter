package com.minewaku.chatter.identityaccess.authentication.internal.port;

public interface RefreshTokenGenerator {

    /** Returns a nonblank opaque credential from 32 cryptographically random bytes. */
    String generate();
}
