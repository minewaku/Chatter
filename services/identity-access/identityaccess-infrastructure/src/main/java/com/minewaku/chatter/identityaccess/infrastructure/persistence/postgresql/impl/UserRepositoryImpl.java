package com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.impl;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.JdbcUserRepository;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.mapper.JdbcUserMapper;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class UserRepositoryImpl implements UserRepository {

	private JdbcUserRepository jdbcUserRepository;
	private JdbcUserMapper jdbcUserMapper;

	@Override
	public void save(User user) {
		jdbcUserRepository.save(jdbcUserMapper.domainToEntity(user));
	}

	@Override
	public void delete(User user) {
		jdbcUserRepository.delete(jdbcUserMapper.domainToEntity(user));
	}

	@Override
	public void deleteById(UserId userId) {
		jdbcUserRepository.deleteById(userId.getValue());
	}

	@Override
	public Optional<User> findById(UserId userId) {
		return jdbcUserRepository.findById(userId.getValue()).map(jdbcUserMapper::entityToDomain);
	}

	@Override
	public Optional<User> findByEmail(Email email) {
		return jdbcUserRepository.findByEmail(email.getValue()).map(jdbcUserMapper::entityToDomain);
	}
}