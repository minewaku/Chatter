package com.minewaku.chatter.identityaccess.application.port.outbound.provider;

public interface UrlGenerator {
    interface ConfirmationTokenUrlGenerator {
        public String generate(String token);
    }
}
