package com.minewaku.chatter.profile.domain.model.file.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import com.minewaku.chatter.profile.domain.model.file.event.AssetOrphanedDomainEvent;
import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.profile.domain.sharedkernel.value.AuditMetadata;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;


@Getter
@Table("asset")
@ToString
public class Asset implements Persistable<AssetId> {

    @Id
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
    private Integer refCount;

    @Embedded.Nullable
    private AuditMetadata auditMetadata;

    @Transient
    private List<DomainEvent> domainEvents = new ArrayList<>();


    @PersistenceCreator
    private Asset(
                @NonNull AssetId id,
                @NonNull Namespace namespace,
                @NonNull String fileHash, 
                @NonNull AssetDimension dimension,
                @NonNull Integer fileSize,
                @NonNull Integer refCount,
                @NonNull AuditMetadata auditMetadata) {

        this.id = id;
        this.namespace = namespace;
        this.fileHash = fileHash;
        this.dimension = dimension;
        this.fileSize = fileSize;
        this.refCount = refCount;
        this.auditMetadata = auditMetadata;
    }

    public static Asset createNew(
            @NonNull AssetId assetId,
                @NonNull Namespace namespace,
                @NonNull String fileHash, 
                @NonNull AssetDimension dimension,
                @NonNull Integer fileSize) {

        return new Asset(
            assetId,
            namespace,
            fileHash,
            dimension,
            fileSize,
            1,
            AuditMetadata.createNew()
        );
    }

    @Override
    public boolean isNew() {
        return true;
    }

    public void detached() {
        if (this.refCount > 0) {
            this.refCount -= 1;
            this.auditMetadata.markUpdated();
        }

        if (this.refCount == 0) {
            AssetOrphanedDomainEvent event = new AssetOrphanedDomainEvent(this.id, this.fileHash, this.namespace);
            this.domainEvents.add(event);
        }
    }

    public void attached() {
        this.refCount += 1;
        if (this.auditMetadata != null) {
            this.auditMetadata.markUpdated();
        }
    }

    public boolean isOrphaned() {
        return this.refCount == 0;
    }
}
