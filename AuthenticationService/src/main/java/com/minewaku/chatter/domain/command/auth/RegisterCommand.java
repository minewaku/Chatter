package com.minewaku.chatter.domain.command.auth;

import com.minewaku.chatter.domain.value.Birthday;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.Password;
import com.minewaku.chatter.domain.value.Username;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record RegisterCommand(
    @NonNull Email email, 
    @NonNull Username username, 
    @NonNull Birthday birthday, 
    @NonNull Password password
) {}
