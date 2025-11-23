package com.minewaku.chatter.adapter.db.mysql.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.db.mysql.JpaUserRepository;
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
	public Optional<User> findByIdAndIsDeletedFalse(UserId id) {
		return userMapper.entityToDomain(jpaUserRepository.findByIdAndIsDeletedFalse(id.getValue()));
	}

	@Override
	public Optional<User> findByEmailAndIsDeletedFalse(Email email) {
		return userMapper.entityToDomain(jpaUserRepository.findByEmailAndIsDeletedFalse(email.getValue()));
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
	public void enable(UserId userId) {
		jpaUserRepository.enableUser(userId.getValue());
	}

	@Override
	public void softDeleteById(UserId userId) {
		jpaUserRepository.softDeleteById(userId.getValue());
	}


	@Override
	public void hardDeleteById(UserId userId) {
		 jpaUserRepository.hardDeleteById(userId.getValue());
	}

	@Override
	public void restoreById(UserId userId) {
		jpaUserRepository.restoreById(userId.getValue());
	}

	@Override
	public void disable(UserId userId) {
		jpaUserRepository.disableUser(userId.getValue());
	}

}
