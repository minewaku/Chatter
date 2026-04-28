package com.minewaku.chatter.profile.domain.model.file.model;

import org.springframework.data.annotation.PersistenceCreator;

import com.minewaku.chatter.profile.domain.sharedkernel.exception.DomainValidationException;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssetDimension {

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
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