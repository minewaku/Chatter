package com.minewaku.chatter.message.domain.model.asset.model;

import org.springframework.data.annotation.PersistenceCreator;

import com.minewaku.chatter.message.domain.sharedkernel.exception.DomainValidationException;
import com.minewaku.chatter.message.domain.sharedkernel.value.Id;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@Getter
@EqualsAndHashCode
public class AssetId implements Id {

    private Long value;

	@PersistenceCreator
    public AssetId(@NonNull Long value) {

		if(Long.valueOf(value) <= 0) {
			throw new DomainValidationException("AttachmentId value cannot be smaller than 1");
		}
		
		this.value = value;
	}
}