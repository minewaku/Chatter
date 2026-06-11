package com.minewaku.chatter.message.infrastructure.persistence.postgresql;

import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import com.minewaku.chatter.message.domain.model.asset.model.Asset;
import com.minewaku.chatter.message.domain.model.asset.model.AssetId;

public interface AssetJdbcRepository extends ListCrudRepository<Asset, AssetId> {

    @Query("SELECT * FROM asset WHERE file_hash = :fileHash AND namespace = :namespace FOR UPDATE")
    Optional<Asset> findByFileHashAndNamespaceForUpdate(
            @Param("fileHash") String fileHash, 
            @Param("namespace") String namespace
    );

    @Modifying
    @Query("DELETE FROM asset WHERE file_hash = :fileHash AND namespace = :namespace")
    void deleteByFileHashAndNamespace(
        @Param("fileHash") String fileHash, 
        @Param("namespace") String namespace
    );
}

