package com.minewaku.chatter.message.infrastructure.persistence.postgresql;

import org.springframework.data.repository.ListCrudRepository;

import com.minewaku.chatter.message.domain.model.asset.model.Asset;
import com.minewaku.chatter.message.domain.model.asset.model.AssetId;

public interface AssetJdbcRepository extends ListCrudRepository<Asset, AssetId>{
    
    // List<Asset> findAllByFileHashIn(Iterable<String> fileHashes);
}
