package com.minewaku.chatter.adapter.db.postgresql.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.db.postgresql.JpaCredentialsRepository;
import com.minewaku.chatter.adapter.mapper.CredentialsMapper;
import com.minewaku.chatter.domain.model.Credentials;
import com.minewaku.chatter.domain.port.out.repository.CredentialsRepository;
import com.minewaku.chatter.domain.value.id.UserId;

@Repository
public class CredentialsRepositoryAdapter implements CredentialsRepository {

	@Autowired
	private JpaCredentialsRepository jpaCredentialsRepository;

	@Autowired
	private CredentialsMapper credentialsMapper;

	@Override
	public Credentials save(Credentials credentials) {
		return credentialsMapper
				.entityToDomain(jpaCredentialsRepository.save(credentialsMapper.domainToEntity(credentials)));
	}

	@Override
	public Optional<Credentials> findById(UserId id) {
		return credentialsMapper.entityToDomain(jpaCredentialsRepository.findById(id.getValue()));
	}

}
