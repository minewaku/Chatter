package com.minewaku.chatter.profile.domain.model.asset.model;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class AssetIdentity {
    
    @Column("namespace")
    private Namespace namespace;

    @Column("file_hash")
    private String fileHash; 

    @PersistenceCreator
    public AssetIdentity(
                @NonNull Namespace namespace,
                @NonNull String fileHash) {
                    
        this.namespace = namespace;
        this.fileHash = fileHash;
    }
}
