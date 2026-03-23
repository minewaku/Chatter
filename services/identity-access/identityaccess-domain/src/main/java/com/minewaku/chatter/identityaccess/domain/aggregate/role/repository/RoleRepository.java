package com.minewaku.chatter.identityaccess.domain.aggregate.role.repository;

import java.util.Optional;

import com.minewaku.chatter.identityaccess.domain.aggregate.role.model.role.Code;
import com.minewaku.chatter.identityaccess.domain.aggregate.role.model.role.Role;
import com.minewaku.chatter.identityaccess.domain.aggregate.role.model.role.RoleId;

public interface RoleRepository {
    Role save(Role role);
    void update(Role role);
    void delete(Role role);
    void deleteById(RoleId id);
    void deleteByIds(Iterable<RoleId> ids);
    Optional<Role> findById(RoleId id);
    Optional<Role> findByCode(Code code);
}
