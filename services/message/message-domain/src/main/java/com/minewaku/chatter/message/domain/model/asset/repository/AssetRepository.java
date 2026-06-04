package com.minewaku.chatter.message.domain.model.asset.repository;

import com.minewaku.chatter.message.domain.model.asset.model.Asset;

public interface AssetRepository {
    void save(Asset asset);
    void saveAll(Iterable<Asset> assets);
    // List<Asset> findAllByFileHashIn(Iterable<String> fileHashes);
}
