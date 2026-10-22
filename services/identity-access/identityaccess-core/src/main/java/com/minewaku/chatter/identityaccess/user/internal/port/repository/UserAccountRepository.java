package com.minewaku.chatter.identityaccess.user.internal.port.repository;

import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.model.Username;
import java.util.Optional;

public interface UserAccountRepository {

    Optional<UserAccount> findById(UserId userId);

    Optional<UserAccount> findByEmail(Email email);

    Optional<UserAccount> findByUsername(Username username);

    UserAccount save(UserAccount account);
}
