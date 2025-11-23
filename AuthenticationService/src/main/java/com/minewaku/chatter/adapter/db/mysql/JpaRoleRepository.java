package com.minewaku.chatter.adapter.db.mysql;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.entity.JpaRoleEntity;

@Repository
public interface JpaRoleRepository extends JpaRepository<JpaRoleEntity, Long> {

    Page<JpaRoleEntity> findAll(Pageable pageable);
	
	@Modifying
	@Query("UPDATE JpaRoleEntity u SET u.name = :#{#jpaRoleEntity.name}, u.description = :#{#jpaRoleEntity.description} WHERE u.id = :#{#jpaRoleEntity.id}")
	void update(JpaRoleEntity jpaRoleEntity);
	
	Optional<JpaRoleEntity> findById(long id);
    
    @Modifying
    @Query("UPDATE JpaRoleEntity u SET u.isDeleted = true WHERE u.id = :id")
    void softDeleteById(@Param("id") Long id);
    
    @Modifying
    @Query("UPDATE JpaRoleEntity u SET u.isDeleted = false WHERE u.id = :id")
    void restoreById(@Param("id") Long id);
    
    @Modifying
    @Query("DELETE FROM JpaRoleEntity u WHERE u.id IN :id")
    void hardDeleteById(@Param("ids") Long ids);
    
    @Modifying
    @Query("DELETE FROM JpaRoleEntity u WHERE u.id IN :ids")
    void bulkHardDeleteByIds(@Param("ids") Iterable<Long> ids);
}
