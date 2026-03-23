package com.minewaku.chatter.identityaccess.infrastructure.persistence.impl;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.SessionId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.JpaUserRepository;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.mapper.UserMapper;

@Repository
public class UserRepositoryImpl implements UserRepository {

	private JpaUserRepository jpaUserRepository;
	private UserMapper userMapper;

	public UserRepositoryImpl(JpaUserRepository jpaUserRepository, UserMapper userMapper) {
		this.jpaUserRepository = jpaUserRepository;
		this.userMapper = userMapper;
	}

	@Override
	public void save(User user) {
		jpaUserRepository.save(userMapper.domainToEntity(user));
	}

	@Override
	public void delete(User user) {
		jpaUserRepository.delete(userMapper.domainToEntity(user));
	}

	@Override
	public void deleteById(UserId userId) {
		jpaUserRepository.deleteById(userId.getValue());
	}

	@Override
	public Optional<User> findById(UserId userId) {
		return jpaUserRepository.findById(userId.getValue()).map(userMapper::entityToDomain);
	}

	@Override
	public Optional<User> findByEmail(Email email) {
		return jpaUserRepository.findByEmail(email.getValue()).map(userMapper::entityToDomain);
	}

	@Override
	public Optional<User> findBySessionId(SessionId sessionId) {
		return jpaUserRepository.findBySessionsSessionId(sessionId.getValue()).map(userMapper::entityToDomain);
	}
}