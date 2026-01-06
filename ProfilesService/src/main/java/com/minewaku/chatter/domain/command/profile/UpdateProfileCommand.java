package com.minewaku.chatter.domain.command.profile;

import com.minewaku.chatter.domain.value.Bio;
import com.minewaku.chatter.domain.value.DisplayName;
import com.minewaku.chatter.domain.value.id.UserId;

import lombok.NonNull;

public record UpdateProfileCommand(@NonNull UserId userId, DisplayName displayName, Bio bio) {}
