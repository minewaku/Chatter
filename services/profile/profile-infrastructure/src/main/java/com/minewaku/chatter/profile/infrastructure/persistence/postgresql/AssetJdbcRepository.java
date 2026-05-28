package com.minewaku.chatter.profile.infrastructure.persistence.postgresql;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import com.minewaku.chatter.profile.domain.model.file.model.Asset;
import com.minewaku.chatter.profile.domain.model.file.model.AssetId;

public interface AssetJdbcRepository extends ListCrudRepository<Asset, AssetId> {
    
    void deleteByFileHash(String hash);

    @Query("SELECT * FROM asset WHERE file_hash IN (:hashFiles) FOR UPDATE")
    List<Asset> findAllByFileHashInForUpdate(@Param("hashFiles") List<String> hashFiles);
}
