package com.minewaku.chatter.profile.infrastructure.persistence.postgresql.impl;

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
}
