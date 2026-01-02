package com.minewaku.chatter.domain.port.in.profile;

import com.minewaku.chatter.domain.command.profile.UpdateProfileCommand;
import com.minewaku.chatter.domain.port.in.UseCaseHandler;

public interface UpdateProfileUseCase extends UseCaseHandler<UpdateProfileCommand, Void> {

}
