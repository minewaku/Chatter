package com.minewaku.chatter.adapter.web.response;

import java.time.Instant;
import java.time.LocalDate;

public record UserDTO(
		long id,
		String email,
		String username,
		LocalDate birthDay,
		boolean enabled,
		boolean locked,
		boolean deleted,
		Instant deletedAt,
		Instant createdAt,
		Instant modifiedAt
) {

}
