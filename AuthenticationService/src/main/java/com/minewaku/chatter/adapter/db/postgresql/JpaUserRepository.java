package com.minewaku.chatter.adapter.db.postgresql;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.entity.JpaUserEntity;

@Repository
public interface JpaUserRepository extends JpaRepository<JpaUserEntity, Long> {

    Optional<JpaUserEntity> findById(Long id);

    Optional<JpaUserEntity> findByEmail(String email);

    @Query("SELECT FROM JpaUserEntity u WHERE u.id = :id AND u.enabled = true AND u.locked = false AND u.isDeleted = false")
    Optional<JpaUserEntity> findByIdAndIsActivated(Long userId);

    Page<JpaUserEntity> findAll(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE JpaUserEntity u SET u.isEnabled = true WHERE u.id = :id")
    void enableUser(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE JpaUserEntity u SET u.isEnabled = false WHERE u.id = :id")
    void disableUser(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE JpaUserEntity u SET u.isLocked = true WHERE u.id = :id")
    void lockUser(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE JpaUserEntity u SET u.isLocked = false WHERE u.id = :id")
    void unlockUser(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM JpaUserEntity u WHERE u.id = :id")
    void hardDeleteById(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM JpaUserEntity u WHERE u.id IN :ids")
    void bulkHardDeleteByIds(@Param("ids") Iterable<Long> ids);
}
