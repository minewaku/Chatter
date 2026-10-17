package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.user.write.jdbc;

import com.minewaku.chatter.identityaccess.user.api.exception.DuplicateUserAccountException;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.model.Username;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserAccountRepository implements UserAccountRepository {

    private final UserAccountRows rows;
    private final UserAccountRowMapper mapper;

    JdbcUserAccountRepository(UserAccountRows rows, UserAccountRowMapper mapper) {
        this.rows = rows;
        this.mapper = mapper;
    }

    @Override
    public Optional<UserAccount> findById(UserId userId) {
        return rows.findById(userId.value()).map(mapper::toModel);
    }

    @Override
    public Optional<UserAccount> findByEmail(Email email) {
        return rows.findByNormalizedEmail(email.value()).map(mapper::toModel);
    }

    @Override
    public Optional<UserAccount> findByUsername(Username username) {
        return rows.findByUsername(username.value()).map(mapper::toModel);
    }

    @Override
    public UserAccount save(UserAccount account) {
        try {
            return mapper.toModel(rows.save(mapper.toRow(account)));
        } catch (RuntimeException e) {
            if (isDuplicateKey(e)) {
                throw new DuplicateUserAccountException("User account already exists", e);
            }
            throw e;
        }
    }

    private static boolean isDuplicateKey(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof DuplicateKeyException) {
                return true;
            }
        }
        return false;
    }
}
