package com.minewaku.chatter.adapter.db.mysql;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.entity.JpaUserEntity;
import com.minewaku.chatter.domain.value.id.UserId;

@Repository
public interface JpaUserRepository extends JpaRepository<JpaUserEntity, Long> {
    
	Optional<JpaUserEntity> findById(UserId id);
	Optional<JpaUserEntity> findByEmail(String email);
	Optional<JpaUserEntity> findByIdAndIsDeletedFalse(Long userId);
	Optional<JpaUserEntity> findByEmailAndIsDeletedFalse(String username);
    Page<JpaUserEntity> findAll(Pageable pageable);
	
    @Modifying
    @Query("UPDATE JpaUserEntity u SET u.isEnabled = true WHERE u.id = :id")
    void enableUser(@Param("id") Long id);
    
    @Modifying
    @Query("UPDATE JpaUserEntity u SET u.isEnabled = false WHERE u.id = :id")
    void disableUser(@Param("id") Long id);
    
    @Modifying
    @Query("UPDATE JpaUserEntity u SET u.isDeleted = true WHERE u.id = :id")
    void softDeleteById(@Param("id") Long id);
    
    @Modifying
    @Query("UPDATE JpaUserEntity u SET u.isDeleted = false WHERE u.id = :id")
    void restoreById(@Param("id") Long id);
    
    @Modifying
    @Query("DELETE FROM JpaUserEntity u WHERE u.id IN :id")
    void hardDeleteById(@Param("ids") Long ids);
    
    @Modifying
    @Query("DELETE FROM JpaUserEntity u WHERE u.id IN :ids")
    void bulkHardDeleteByIds(@Param("ids") Iterable<Long> ids);
}
