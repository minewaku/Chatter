package com.minewaku.chatter.domain.command.auth;

import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.Password;

import lombok.Builder;

import lombok.NonNull;

@Builder
public record ChangePasswordCommand(
    @NonNull Email email,
    @NonNull Password password,
    @NonNull Password newPassword
) {}