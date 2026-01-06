package com.minewaku.chatter.domain.port.in.profile;

import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.UseCaseHandler;
import com.minewaku.chatter.domain.value.Username;

public interface FindActivatedProfileByUsernameUseCase extends UseCaseHandler<Username, User>{
    
}
