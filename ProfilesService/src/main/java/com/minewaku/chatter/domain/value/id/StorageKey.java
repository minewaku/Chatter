package com.minewaku.chatter.domain.value.id;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class StorageKey {
	
	@NonNull
	private final String value;
	
	@JsonCreator
	public StorageKey(@NonNull String value) {
		this.value = value;
	}
}
