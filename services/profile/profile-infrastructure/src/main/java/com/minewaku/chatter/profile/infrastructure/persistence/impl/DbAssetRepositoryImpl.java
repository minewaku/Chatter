package com.minewaku.chatter.profile.infrastructure.persistence.impl;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.profile.domain.model.file.model.Asset;
import com.minewaku.chatter.profile.domain.model.file.model.AssetId;
import com.minewaku.chatter.profile.domain.model.file.repository.AssetRepository;
import com.minewaku.chatter.profile.infrastructure.persistence.JpaAssetRepository;

@Component
public class DbAssetRepositoryImpl implements AssetRepository{

    private final JpaAssetRepository jpaAssetRepository;

    public DbAssetRepositoryImpl(
            JpaAssetRepository jpaAssetRepository) {

        this.jpaAssetRepository = jpaAssetRepository;
    }

    @Override
    public void save(Asset file) {
        jpaAssetRepository.save(file);
    }

    @Override
    public void delete(Asset file) {
        jpaAssetRepository.delete(file);
    }

    @Override
    public void deleteByFileHash(String hash) {
        jpaAssetRepository.deleteByFileHash(hash);
    }

    @Override
    public Optional<Asset> findById(AssetId assetId) {
        return jpaAssetRepository.findById(assetId);
    }
    
}
