package com.minewaku.chatter.profile.domain.model.file.model;

import java.io.Serializable;

import org.springframework.data.relational.core.mapping.Column;

import com.minewaku.chatter.profile.domain.sharedkernel.exception.DomainValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssetId implements Serializable {

	@Column("id")
    private Long value;

    public AssetId(@NonNull Long value) {

		if(Long.valueOf(value) <= 0) {
			throw new DomainValidationException("AssetId value cannot be smaller than 1");
		}
		
		this.value = value;
	}
}