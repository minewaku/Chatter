package com.minewaku.chatter.profile.domain.model.asset.model;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;

import com.minewaku.chatter.profile.domain.sharedkernel.exception.DomainValidationException;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class AssetDimension {

    @Column("width")
    private Integer width;

    @Column("height")
    private Integer height;

    @PersistenceCreator
    public AssetDimension(
                @NonNull Integer width, 
                @NonNull Integer height) {

        if (width != null && width < 0) throw new DomainValidationException("Width must be >= 0");
        if (height != null && height < 0) throw new DomainValidationException("Height must be >= 0");
        this.width = width;
        this.height = height;
    }
}