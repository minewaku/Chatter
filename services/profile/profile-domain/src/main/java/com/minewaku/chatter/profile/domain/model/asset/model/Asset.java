package com.minewaku.chatter.profile.domain.model.asset.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import com.minewaku.chatter.profile.domain.model.asset.event.AssetOrphanedDomainEvent;
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

    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
    private AssetIdentity identity;

    @Column("content_type")
    private String contentType;

    @Column("file_name")
    private String fileName;

    @Column("file_size")
    private Integer fileSize;

    @Column("ref_count")
    private Integer refCount;

    @Embedded.Nullable
    private AuditMetadata auditMetadata;

    @Transient
    private boolean isNew = false;

    @Transient
    private List<DomainEvent> domainEvents = new ArrayList<>();

    @PersistenceCreator
    private Asset(
                @NonNull AssetId id,
                @NonNull AssetIdentity identity,
                @NonNull String contentType,
                @NonNull String fileName,
                @NonNull Integer fileSize,
                @NonNull Integer refCount,
                @NonNull AuditMetadata auditMetadata) {

        this.id = id;
        this.identity = identity;
        this.contentType = contentType;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.refCount = refCount;
        this.auditMetadata = auditMetadata;
    }

    public static Asset createNew(
            @NonNull AssetId assetId,
            @NonNull Namespace namespace,
            @NonNull String fileName,
            @NonNull String fileHash, 
            @NonNull String contentType,
            @NonNull Integer fileSize) {

        Asset asset = new Asset(
            assetId,
            new AssetIdentity(namespace, fileHash),
            contentType,
            fileName,
            fileSize,
            1,
            AuditMetadata.createNew()
        );

        asset.isNew = true;
        return asset;
    }

    @Override
    public boolean isNew() {
        return this.isNew;
    }

    public void detached() {
        if (this.refCount > 0) {
            this.refCount -= 1;
            this.auditMetadata.markUpdated();
        }

        if (this.refCount == 0) {
            AssetOrphanedDomainEvent event = new AssetOrphanedDomainEvent(
                this.id, 
                this.identity.getFileHash(), 
                this.identity.getNamespace()
            );
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