package com.minewaku.chatter.message.infrastructure.persistence.postgresql.impl;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.message.domain.model.asset.model.Asset;
import com.minewaku.chatter.message.domain.model.asset.repository.AssetRepository;
import com.minewaku.chatter.message.infrastructure.persistence.postgresql.AssetJdbcRepository;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class AssetRepositoryImpl implements AssetRepository {
    
    private final AssetJdbcRepository assetJdbcRepository;

    @Override
    public void save(Asset asset) {
        assetJdbcRepository.save(asset);
    }

    @Override
    public void saveAll(Iterable<Asset> assets) {
        assetJdbcRepository.saveAll(assets);
    }

    //recheck
    // @Override
    // public List<Asset> findAllByFileHashIn(Iterable<String> fileHashes) {
    //     return assetJdbcRepository.findAllByFileHashIn(fileHashes);
    // }
}
