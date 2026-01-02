package com.minewaku.chatter.application.service.profile;

import com.minewaku.chatter.domain.command.profile.UpdateProfileCommand;
import com.minewaku.chatter.domain.port.in.profile.UpdateProfileUseCase;

public class UpdateProfileApplicationService implements UpdateProfileUseCase {
    
    @Override
    public Void handle(UpdateProfileCommand command) {
        throw new UnsupportedOperationException("Unimplemented method 'handle'");
    }
}
