package com.minewaku.chatter.adapter.db.mysql;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.entity.JpaRoleEntity;
import com.minewaku.chatter.adapter.entity.JpaUserRoleEntity;
import com.minewaku.chatter.adapter.entity.embededKey.JpaUserRoleId;

@Repository
public interface JpaUserRoleRepository extends JpaRepository<JpaUserRoleEntity, JpaUserRoleId> {
	void deleteById(JpaUserRoleId userRoleId);
	
	@Query("""
	        SELECT ur.role
	        FROM JpaUserRoleEntity ur
	        WHERE ur.user.id = :userId
	          AND ur.isDeleted = false
	    """)
	Set<JpaRoleEntity> findRolesByUserIdAndIsDeletedFalse(@Param("userId") Long userId);
}
