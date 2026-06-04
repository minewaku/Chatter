package com.minewaku.chatter.profile.infrastructure.persistence.postgresql.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.minewaku.chatter.profile.domain.model.asset.model.Asset;
import com.minewaku.chatter.profile.domain.model.asset.model.AssetId;
import com.minewaku.chatter.profile.domain.model.asset.model.AssetIdentity;
import com.minewaku.chatter.profile.domain.model.asset.repository.AssetRepository;
import com.minewaku.chatter.profile.infrastructure.persistence.postgresql.AssetJdbcRepository;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class AssetRepositoryImpl implements AssetRepository {

    private final AssetJdbcRepository assetJdbcRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void save(Asset file) {
        assetJdbcRepository.save(file);
    }

    @Override
    public void saveAll(Collection<Asset> assets) {
        if (assets == null || assets.isEmpty()) {
            return;
        }
        assetJdbcRepository.saveAll(assets);
    }

    @Override
    public void delete(Asset file) {
        assetJdbcRepository.delete(file);
    }

    @Override
    public void deleteAllByIds(Collection<AssetId> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return;
        }
        assetJdbcRepository.deleteAllById(assetIds);
    }

    @Override
    public void deleteByAssetIdentity(AssetIdentity assetIdentity) {
        assetJdbcRepository.deleteByFileHashAndNamespace(
                assetIdentity.getFileHash(), 
                assetIdentity.getNamespace().name()
        );
    }

    @Override
    public Optional<Asset> findById(AssetId assetId) {
        return assetJdbcRepository.findById(assetId);
    }

    @Override
    public Optional<Asset> findByAssetIdentity(AssetIdentity assetIdentity) {
        return assetJdbcRepository.findByFileHashAndNamespaceForUpdate(
                assetIdentity.getFileHash(), 
                assetIdentity.getNamespace().name()
        );
    }

    @Override
    public List<Asset> findAllByAssetIdentities(Collection<AssetIdentity> assetIdentities) {
        if (assetIdentities == null || assetIdentities.isEmpty()) {
            return Collections.emptyList();
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM asset WHERE (file_hash, namespace) IN (");
        MapSqlParameterSource parameters = new MapSqlParameterSource();

        int i = 0;
        for (AssetIdentity identity : assetIdentities) {
            String hashParam = "hash" + i;
            String nsParam = "ns" + i;

            sql.append(String.format("(:%s, :%s),", hashParam, nsParam));
            parameters.addValue(hashParam, identity.getFileHash());
            parameters.addValue(nsParam, identity.getNamespace().name());
            i++;
        }
        
        sql.deleteCharAt(sql.length() - 1).append(") FOR UPDATE");

        return jdbcTemplate.query(sql.toString(), parameters, new DataClassRowMapper<>(Asset.class));
    }
}