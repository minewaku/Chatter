package com.minewaku.chatter.adapter.db.postgresql.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.db.postgresql.JpaUserRepository;
import com.minewaku.chatter.adapter.mapper.UserMapper;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.id.UserId;

@Repository
public class UserRepositoryAdapter implements UserRepository {

	@Autowired
	private JpaUserRepository jpaUserRepository;

	@Autowired
	private UserMapper userMapper;

	@Override
	public User save(User user) {
		return userMapper.entityToDomain(jpaUserRepository.save(userMapper.domainToEntity(user)));
	}

	@Override
	public void update(User user) {
		jpaUserRepository.save(userMapper.domainToEntity(user));
	}

	@Override
	public Optional<User> findById(UserId id) {
		return userMapper.entityToDomain(jpaUserRepository.findById(id.getValue()));
	}

	@Override
	public Optional<User> findByEmail(Email email) {
		return userMapper.entityToDomain(jpaUserRepository.findByEmail(email.getValue()));
	}

	@Override
	public void enable(User user) {
		jpaUserRepository.enableUser(user.getId().getValue());
	}

	@Override
	public void softDelete(User user) {
		jpaUserRepository.softDeleteById(user.getId().getValue());
	}

	@Override
	public void hardDeleteById(UserId userId) {
		jpaUserRepository.hardDeleteById(userId.getValue());
	}

	@Override
	public void restore(User user) {
		jpaUserRepository.restoreById(user.getId().getValue());
	}

	@Override
	public void disable(User user) {
		jpaUserRepository.disableUser(user.getId().getValue());
	}

	@Override
	public void lock(User user) {
		jpaUserRepository.lockUser(user.getId().getValue());
	}

	@Override
	public void unlock(User user) {
		jpaUserRepository.unlockUser(user.getId().getValue());
	}

}
