package com.minewaku.chatter.identityaccess.authentication.api.command;

import com.minewaku.chatter.identityaccess.shared.handler.UseCaseHandler;

public interface AuthenticateUseCase
        extends UseCaseHandler<AuthenticateUseCase.Command, AuthenticateUseCase.Authenticated> {

    record Command(String email, String password, DeviceInfo deviceInfo) {}

    record DeviceInfo(String label) {

        public DeviceInfo {
            if (label == null || label.isBlank() || label.length() > 200) {
                throw new IllegalArgumentException("Device label must be nonblank and at most 200 characters");
            }
        }
    }

    record Authenticated(String accessToken, String refreshToken) {}
}
