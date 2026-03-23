package com.minewaku.chatter.identityaccess.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.identityaccess.infrastructure.entity.JpaOutboxEntity;

@Repository
public interface JpaOutboxRepository extends JpaRepository<JpaOutboxEntity, Long> {

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM JpaOutboxEntity o WHERE o.createdAt < :cutoffDateTime")
	void deleteByCreatedAtBefore(java.time.LocalDateTime cutoffDateTime);
}
