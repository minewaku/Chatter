package com.minewaku.chatter.domain.value.id;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class ConfirmationCode {

    @NonNull
	private final String value;
	
	public ConfirmationCode(@NonNull String value) {
		this.value = value;
	}
}
