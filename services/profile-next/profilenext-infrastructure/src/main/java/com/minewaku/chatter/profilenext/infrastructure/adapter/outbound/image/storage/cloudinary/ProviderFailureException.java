package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.cloudinary;

/** A call to the provider failed. Never carries a request or a credential. */
class ProviderFailureException extends RuntimeException {

    ProviderFailureException(String message) {
        super(message);
    }

    ProviderFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
