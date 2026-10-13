package com.minewaku.chatter.identityaccess.authentication.internal.application.query;

import com.minewaku.chatter.identityaccess.authentication.api.query.FindSessionsQuery;
import com.minewaku.chatter.identityaccess.authentication.internal.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.authentication.internal.port.query.AuthenticationSessionViewRepository;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindSessionsService implements FindSessionsQuery {

    @NonNull private final UserAccountRepository userRepository;

    @NonNull private final AuthenticationSessionViewRepository views;

    @Override
    @Transactional(readOnly = true)
    public List<SessionView> handle(Command command) {
        UserAccount account = userRepository
                .findById(new UserId(command.userId()))
                .filter(UserAccount::isAccessible)
                .orElseThrow(AuthenticationFailedException::new);
        return views.findByUserId(account.id());
    }
}
