package com.minewaku.chatter.profile.domain.model.file.repository;

import java.util.List;
import java.util.Optional;

import com.minewaku.chatter.profile.domain.model.file.model.Asset;
import com.minewaku.chatter.profile.domain.model.file.model.AssetId;

public interface AssetRepository {
    void save(Asset file);
    void delete(Asset file);
    void deleteByFileHash(String hash);
    Optional<Asset> findById(AssetId assetId);
    void saveAll(Iterable<Asset> assets);
    List<Asset> findAllByFileHashInForUpdate(List<String> hashFiles);
    void deleteAllByIdIn(List<AssetId> assetIds);
}
