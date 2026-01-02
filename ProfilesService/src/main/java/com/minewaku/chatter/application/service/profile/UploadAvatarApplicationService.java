package com.minewaku.chatter.application.service.profile;

import com.minewaku.chatter.domain.port.in.profile.UploadAvatarUseCase;
import com.minewaku.chatter.domain.value.InputFile;

public class UploadAvatarApplicationService implements UploadAvatarUseCase {
    @Override
    public Void handle(InputFile command) {
        throw new UnsupportedOperationException("Unimplemented method 'handle'");
    }
}
