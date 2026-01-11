package com.minewaku.chatter.adapter.web.response;

import java.time.Instant;
import java.time.LocalDate;

public record UserDto(
		long id,
		String email,
		String avatar,
		String cover,
		String username,
		String displayName,
		String bio,
		LocalDate birthday,
		boolean discoverable,
		boolean enabled,
		boolean locked,
		boolean deleted,
		Instant deletedAt,
		Instant createdAt,
		Instant modifiedAt
) {

}
