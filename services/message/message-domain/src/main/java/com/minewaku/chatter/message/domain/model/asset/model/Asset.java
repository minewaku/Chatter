package com.minewaku.chatter.message.domain.model.asset.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import com.minewaku.chatter.message.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.message.domain.sharedkernel.exception.DomainValidationException;
import com.minewaku.chatter.message.domain.sharedkernel.value.BaseEntity;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@Table("asset")
@ToString
public class Asset extends BaseEntity<AssetId> {

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

    @Column("ref_count")    
    private Integer refCount = 0;

    @Version
    @Column("version")
    private Long version;

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    @PersistenceCreator
    public Asset(
                @NonNull AssetId attachmentId,
                @NonNull Namespace namespace,
                @NonNull String fileHash, 
                @NonNull AssetDimension dimension,
                @NonNull Integer fileSize,
                Integer refCount,
                Long version) {

        this.id = attachmentId;
        this.namespace = namespace;
        this.fileHash = fileHash;
        this.dimension = dimension;
        this.fileSize = fileSize;
        
        if (refCount < 0) {
            throw new DomainValidationException("refCount cannot be negative");
        }
        this.refCount = refCount == null ? 1 : refCount;
        this.version = version;
    }

    public void increaseRefCount() {
        this.refCount++;
    }

    public void decreaseRefCount() {
        if (this.refCount > 0) {
            this.refCount--;
        }
    }
}

