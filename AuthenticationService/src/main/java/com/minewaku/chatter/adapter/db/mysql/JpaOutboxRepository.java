package com.minewaku.chatter.adapter.db.mysql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.entity.JpaOutboxEntity;

@Repository
public interface JpaOutboxRepository extends JpaRepository<JpaOutboxEntity, Long> {
	
	@Modifying
	@Query("UPDATE JpaOutboxEntity o SET o.isProcessed = true WHERE o.id = :id")
	void markProcessed(Long id);
}
