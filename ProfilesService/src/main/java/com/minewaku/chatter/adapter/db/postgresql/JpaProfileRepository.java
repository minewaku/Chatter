package com.minewaku.chatter.adapter.db.postgresql;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.entity.JpaUserEntity;

@Repository
public interface JpaProfileRepository extends JpaRepository<JpaUserEntity, Long> {

    @Query("SELECT u FROM JpaUserEntity u WHERE u.username = :username AND u.enabled = true AND u.deleted = false AND u.locked = false")
    Optional<JpaUserEntity> findActivatedByUsername(@Param("username") String username);

    @Query("SELECT u FROM JpaUserEntity u WHERE u.id = :id AND u.enabled = true AND u.deleted = false AND u.locked = false")
    Optional<JpaUserEntity> findActivatedByUserId(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE JpaUserEntity u SET u.displayName = :#{#user.displayName}, u.bio = :#{#user.bio} WHERE u.id = :#{#user.id}")
    void updateUser(@Param("user") JpaUserEntity user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE JpaUserEntity u SET u.avatarKey = :#{#user.avatarKey}, u.avatar = :#{#user.avatar} WHERE u.id = :#{#user.id}")
    void uploadAvatar(@Param("user") JpaUserEntity user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE JpaUserEntity u SET u.bannerKey = :#{#user.bannerKey}, u.banner = :#{#user.banner} WHERE u.id = :#{#user.id}")
    void uploadBanner(@Param("user") JpaUserEntity user);
}
