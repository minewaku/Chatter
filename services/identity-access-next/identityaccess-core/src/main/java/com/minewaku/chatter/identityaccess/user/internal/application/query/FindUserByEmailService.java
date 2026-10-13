package com.minewaku.chatter.identityaccess.user.internal.application.query;

import com.minewaku.chatter.identityaccess.user.api.query.FindUserByEmailUseCase;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.port.query.UserAccountQueryRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindUserByEmailService implements FindUserByEmailUseCase {

    private final UserAccountQueryRepository repository;

    @Override
    public Optional<Result> handle(Query query) {
        return repository.findByEmail(new Email(query.email()));
    }
}
