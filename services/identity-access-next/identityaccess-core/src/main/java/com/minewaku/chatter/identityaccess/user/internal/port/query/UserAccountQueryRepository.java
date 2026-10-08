package com.minewaku.chatter.identityaccess.user.internal.port.query;

import com.minewaku.chatter.identityaccess.user.api.query.FindUserByEmailUseCase;
import com.minewaku.chatter.identityaccess.user.api.query.FindUserByIdUseCase;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.util.Optional;

public interface UserAccountQueryRepository {

    Optional<FindUserByIdUseCase.Result> findById(UserId userId);

    Optional<FindUserByEmailUseCase.Result> findByEmail(Email email);
}
