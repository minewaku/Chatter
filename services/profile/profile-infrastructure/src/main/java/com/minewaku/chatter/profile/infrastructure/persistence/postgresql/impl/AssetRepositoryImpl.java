package com.minewaku.chatter.profile.infrastructure.persistence.postgresql.impl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.profile.domain.model.file.model.Asset;
import com.minewaku.chatter.profile.domain.model.file.model.AssetId;
import com.minewaku.chatter.profile.domain.model.file.repository.AssetRepository;
import com.minewaku.chatter.profile.infrastructure.persistence.postgresql.AssetJdbcRepository;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class AssetRepositoryImpl implements AssetRepository {
    
    private final AssetJdbcRepository assetJdbcRepository;

    @Override
    public void save(Asset file) {
        assetJdbcRepository.save(file);
    }

    @Override
    public void delete(Asset file) {
        assetJdbcRepository.delete(file);
    }

    @Override
    public void deleteByFileHash(String hash) {
        assetJdbcRepository.deleteByFileHash(hash);
    }

    @Override
    public Optional<Asset> findById(AssetId assetId) {
        return assetJdbcRepository.findById(assetId);
    }

    @Override
    public void saveAll(Iterable<Asset> assets) {
        if (assets == null || !assets.iterator().hasNext()) {
            return;
        }
        assetJdbcRepository.saveAll(assets);
    }

    @Override
    public List<Asset> findAllByFileHashInForUpdate(List<String> hashFiles) {
        if (hashFiles == null || hashFiles.isEmpty()) {
            return Collections.emptyList();
        }
        return assetJdbcRepository.findAllByFileHashInForUpdate(hashFiles);
    }

    @Override
    public void deleteAllByIdIn(List<AssetId> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return;
        }
        assetJdbcRepository.deleteAllById(assetIds);
    }
}