package com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command;

import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Birthday;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Username;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.Password;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record RegisterCommand(
    @NonNull Email email, 
    @NonNull Username username, 
    @NonNull Birthday birthday, 
    @NonNull Password password
) {}
