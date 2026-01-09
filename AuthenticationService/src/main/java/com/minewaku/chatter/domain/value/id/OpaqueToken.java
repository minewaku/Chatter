package com.minewaku.chatter.domain.value.id;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class OpaqueToken {
    
    @NonNull
	private final String value;
	
	public OpaqueToken(@NonNull String value) {
		this.value = value;
	}
}
