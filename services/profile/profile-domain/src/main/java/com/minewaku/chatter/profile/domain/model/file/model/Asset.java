package com.minewaku.chatter.profile.domain.model.file.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;


@Getter
@Table("asset")
@ToString
public class Asset implements Persistable<AssetId> {

    @Id
    @Embedded.Nullable
    private AssetId id;

    @Column("namespace")
    private Namespace namespace;

    @Column("file_hash")
    private String fileHash; 

    @Embedded.Nullable
    private AssetDimension dimension;

    @Column("file_size")
    private Integer fileSize;

    @PersistenceCreator
    public Asset(
                @NonNull AssetId assetId,
                @NonNull Namespace namespace,
                @NonNull String fileHash, 
                @NonNull AssetDimension dimension,
                @NonNull Integer fileSize) {

        this.id = assetId;
        this.namespace = namespace;
        this.fileHash = fileHash;
        this.dimension = dimension;
        this.fileSize = fileSize;
    }

    @Override
    public boolean isNew() {
        return true;
    }
}
