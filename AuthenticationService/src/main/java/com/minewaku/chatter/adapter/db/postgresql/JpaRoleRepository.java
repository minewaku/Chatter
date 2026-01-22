package com.minewaku.chatter.adapter.db.postgresql;

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
    Optional<JpaRoleEntity> findByCode(String code);
    Optional<JpaRoleEntity> findById(long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE JpaRoleEntity u SET u.name = :#{#jpaRoleEntity.name}, u.description = :#{#jpaRoleEntity.description}, u.modifiedAt = :#{#jpaRoleEntity.modifiedAt} WHERE u.id = :#{#jpaRoleEntity.id}")
    void update(JpaRoleEntity jpaRoleEntity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE JpaRoleEntity u SET u.isDeleted = :#{#jpaRoleEntity.isDeleted}, u.deletedAt = :#{#jpaRoleEntity.deletedAt} WHERE u.id = :#{#jpaRoleEntity.id}")
    void softDelete(JpaRoleEntity jpaRoleEntity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE JpaRoleEntity u SET u.isDeleted = :#{#jpaRoleEntity.isDeleted}, u.deletedAt = :#{#jpaRoleEntity.deletedAt} WHERE u.id = :#{#jpaRoleEntity.id}")
    void restore(JpaRoleEntity jpaRoleEntity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM JpaRoleEntity u WHERE u.id = :id")
    void hardDeleteById(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM JpaRoleEntity u WHERE u.id IN :ids")
    void bulkHardDeleteByIds(@Param("ids") Iterable<Long> ids);
}
