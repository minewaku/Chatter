package com.minewaku.chatter.message.domain.model.asset.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.minewaku.chatter.message.domain.model.asset.model.Asset;
import com.minewaku.chatter.message.domain.model.asset.model.AssetId;
import com.minewaku.chatter.message.domain.model.asset.model.AssetIdentity;

public interface AssetRepository {
    void save(Asset file);
    void saveAll(Collection<Asset> assets);

    void delete(Asset file);
    void deleteAllByIds(Collection<AssetId> assetIds);
    void deleteByAssetIdentity(AssetIdentity assetIdentity);

    Optional<Asset> findById(AssetId assetId);
    Optional<Asset> findByAssetIdentity(AssetIdentity assetIdentity);
    List<Asset> findAllByAssetIdentities(Collection<AssetIdentity> assetIdentities);
}
