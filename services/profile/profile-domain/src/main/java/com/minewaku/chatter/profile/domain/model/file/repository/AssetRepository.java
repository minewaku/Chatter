package com.minewaku.chatter.profile.domain.model.file.repository;

import java.util.Optional;

import com.minewaku.chatter.profile.domain.model.file.model.Asset;
import com.minewaku.chatter.profile.domain.model.file.model.AssetId;

public interface AssetRepository {
    void save(Asset file);
    void delete(Asset file);
    Optional<Asset> findById(AssetId assetId);
}
