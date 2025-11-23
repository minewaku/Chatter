package com.minewaku.chatter.adapter.db.mysql.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.minewaku.chatter.adapter.db.mysql.JpaRoleRepository;
import com.minewaku.chatter.adapter.mapper.RoleMapper;
import com.minewaku.chatter.domain.model.Role;
import com.minewaku.chatter.domain.port.out.repository.RoleRepository;
import com.minewaku.chatter.domain.value.id.RoleId;

@Repository
public class RoleRepositoryAdapter implements RoleRepository {
	
	@Autowired
	private JpaRoleRepository jpaRoleRepository;
	
	@Autowired
	private RoleMapper roleMapper;

	@Override
	public Role save(Role role) {
		return roleMapper.entityToDomain(jpaRoleRepository.save(roleMapper.domainToEntity(role)));
	}

	@Override
	public void update(Role role) {
		jpaRoleRepository.update(roleMapper.domainToEntity(role));
	}

	@Override
	public Optional<Role> findById(RoleId id) {
		return roleMapper.entityToDomain(jpaRoleRepository.findById(id.getValue()));
	}

	@Override
	public void softDeleteById(RoleId roleId) {
		jpaRoleRepository.softDeleteById(roleId.getValue());
	}

	@Override
	public void hardDeleteById(RoleId roleId) {
		jpaRoleRepository.hardDeleteById(roleId.getValue());
	}

	@Override
	public void restoreById(RoleId roleId) {
		jpaRoleRepository.restoreById(roleId.getValue());
	}

}
