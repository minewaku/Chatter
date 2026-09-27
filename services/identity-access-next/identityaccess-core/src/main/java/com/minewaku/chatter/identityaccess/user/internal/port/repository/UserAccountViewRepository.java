package com.minewaku.chatter.identityaccess.user.internal.port.repository;

import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccountView;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.util.Optional;

public interface UserAccountViewRepository {

    Optional<UserAccountView> findById(UserId userId);

    Optional<UserAccountView> findByEmail(Email email);
}
