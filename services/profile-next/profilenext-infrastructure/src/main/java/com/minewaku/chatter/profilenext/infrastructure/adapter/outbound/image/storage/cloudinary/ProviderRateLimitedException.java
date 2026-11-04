package com.minewaku.chatter.profilenext.infrastructure.adapter.outbound.image.storage.cloudinary;

/** The provider answered that the account's hourly call allowance is used up. */
class ProviderRateLimitedException extends ProviderFailureException {

    ProviderRateLimitedException(String message) {
        super(message);
    }
}
