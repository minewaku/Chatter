package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.write.jdbc;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

interface UserAccountRows extends CrudRepository<UserAccountRow, Long> {

    Optional<UserAccountRow> findByNormalizedEmail(String normalizedEmail);

    Optional<UserAccountRow> findByUsername(String username);
}
