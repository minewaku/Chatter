package com.minewaku.chatter.adapter.db.mysql.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.db.mysql.JpaCredentialsRepository;
import com.minewaku.chatter.adapter.mapper.CredentialsMapper;
import com.minewaku.chatter.domain.model.Credentials;
import com.minewaku.chatter.domain.port.out.repository.CredentialsRepository;
import com.minewaku.chatter.domain.value.HashedPassword;
import com.minewaku.chatter.domain.value.id.UserId;

@Repository
public class CredentialsRepositoryAdapter implements CredentialsRepository {
	
	@Autowired
	private JpaCredentialsRepository jpaCredentialsRepository;
	
	@Autowired
	private CredentialsMapper credentialsMapper;

	@Override
	public Credentials save(Credentials credentials) {
		return credentialsMapper.entityToDomain(jpaCredentialsRepository.save(credentialsMapper.domainToEntity(credentials)));
	}

	@Override
	public void changePassword(UserId id, HashedPassword newHashedPassword) {
		jpaCredentialsRepository.changePassword(newHashedPassword.hash(), newHashedPassword.algorithm(), newHashedPassword.salt(), id.getValue());
	}

	@Override
	public Credentials findByUserId(UserId id) {
		return credentialsMapper.entityToDomain(jpaCredentialsRepository.findByUserId(id.getValue()));
	}

}
