package com.minewaku.chatter.profile.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minewaku.chatter.profile.domain.model.file.model.Asset;
import com.minewaku.chatter.profile.domain.model.file.model.AssetId;

public interface JpaAssetRepository extends JpaRepository<Asset, AssetId>{
    void deleteByFileHash(String hash);
}
