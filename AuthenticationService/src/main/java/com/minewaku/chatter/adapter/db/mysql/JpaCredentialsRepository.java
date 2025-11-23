package com.minewaku.chatter.adapter.db.mysql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.entity.JpaCredentialsEntity;

@Repository
public interface JpaCredentialsRepository extends JpaRepository<JpaCredentialsEntity, Long> {
	
	@Modifying
	@Query("""
	    UPDATE JpaCredentialsEntity c 
	    SET c.hashedPassword = :hashedPassword, 
	        c.algorithm = :algorithm, 
	        c.salt = :salt 
	    WHERE c.user.id = :id
	""")
	void changePassword(@Param("hashedPassword") String hashedPassword,
	                    @Param("algorithm") String algorithm,
	                    @Param("salt") byte[] salt,
	                    @Param("id") Long id);
	
	JpaCredentialsEntity findByUserId(long id);
}
