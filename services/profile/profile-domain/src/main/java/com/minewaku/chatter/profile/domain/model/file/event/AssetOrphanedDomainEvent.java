package com.minewaku.chatter.profile.domain.model.file.event;

import com.minewaku.chatter.profile.domain.model.file.model.AssetId;
import com.minewaku.chatter.profile.domain.model.file.model.Namespace;
import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class AssetOrphanedDomainEvent extends DomainEvent {

    private final AssetId assetId;
    private final String hashFile;
    private final Namespace namespace;

    public AssetOrphanedDomainEvent(
            @NonNull AssetId assetId,
            @NonNull String hashFile,
            @NonNull Namespace namespace) {

        this.assetId = assetId; 
        this.hashFile = hashFile;
        this.namespace = namespace;
    }
}
