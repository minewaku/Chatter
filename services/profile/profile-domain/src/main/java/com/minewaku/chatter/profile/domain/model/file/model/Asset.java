package com.minewaku.chatter.profile.domain.model.file.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Entity
@Table(name = "assets", indexes = {
    @Index(name = "idx_asset_hash", columnList = "file_hash") 
})
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Asset {

    @EmbeddedId
    @NonNull
    private AssetId id;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column(name = "namespace", nullable = false, length = 32)
    private Namespace namespace;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash; 

    @Embedded
    private ImageDimension dimension;

    public Asset(
                @NonNull AssetId assetId,
                @NonNull Namespace namespace,
                @NonNull String fileHash, 
                @NonNull ImageDimension dimension) {

        this.id = assetId;
        this.namespace = namespace;
        this.fileHash = fileHash;
        this.dimension = dimension;
    }
}
