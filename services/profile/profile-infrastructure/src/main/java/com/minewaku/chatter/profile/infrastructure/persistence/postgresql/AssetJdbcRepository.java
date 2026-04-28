package com.minewaku.chatter.profile.infrastructure.persistence.postgresql;

import org.springframework.data.repository.ListCrudRepository;

import com.minewaku.chatter.profile.domain.model.file.model.Asset;
import com.minewaku.chatter.profile.domain.model.file.model.AssetId;

public interface AssetJdbcRepository extends ListCrudRepository<Asset, AssetId> {
    void deleteByFileHash(String hash);
}
