package com.minewaku.chatter.adapter.db.postgresql.impl;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.db.postgresql.JpaUserRoleRepository;
import com.minewaku.chatter.adapter.entity.embededKey.JpaUserRoleId;
import com.minewaku.chatter.adapter.mapper.RoleMapper;
import com.minewaku.chatter.adapter.mapper.UserRoleMapper;
import com.minewaku.chatter.domain.model.Role;
import com.minewaku.chatter.domain.model.UserRole;
import com.minewaku.chatter.domain.port.out.repository.UserRoleRepository;
import com.minewaku.chatter.domain.value.id.UserId;
import com.minewaku.chatter.domain.value.id.UserRoleId;

@Repository
public class UserRoleRepositoryAdapter implements UserRoleRepository {

	@Autowired
	private JpaUserRoleRepository jpaUserRoleRepository;

	@Autowired
	private UserRoleMapper userRoleMapper;

	@Autowired
	private RoleMapper roleMapper;

	@Override
	public UserRole save(UserRole userRole) {
		return userRoleMapper.entityToDomain(jpaUserRoleRepository.save(userRoleMapper.domainToEntity(userRole)));
	}

	@Override
	public void deleteById(UserRoleId userRoleId) {
		JpaUserRoleId jpaUserRoleId = new JpaUserRoleId(userRoleId.getUserId().getValue(),
				userRoleId.getRoleId().getValue());
		jpaUserRoleRepository.deleteById(jpaUserRoleId);
	}

	@Override
	public Set<Role> findRolesByUserIdAndIsDeletedFalse(UserId userId) {
		return jpaUserRoleRepository
				.findRolesByUserIdAndIsDeletedFalse(userId.getValue())
				.stream()
				.map(roleMapper::entityToDomain)
				.collect(Collectors.toSet());
	}
}
