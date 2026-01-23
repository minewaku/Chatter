package com.minewaku.chatter.adapter.db.postgresql;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.entity.JpaCredentialsEntity;

@Repository
public interface JpaCredentialsRepository extends JpaRepository<JpaCredentialsEntity, Long> {
	Optional<JpaCredentialsEntity> findById(long id);
}
