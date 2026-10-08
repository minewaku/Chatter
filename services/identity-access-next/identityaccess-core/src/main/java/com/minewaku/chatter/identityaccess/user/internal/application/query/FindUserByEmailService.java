package com.minewaku.chatter.identityaccess.user.internal.application.query;

import com.minewaku.chatter.identityaccess.user.api.query.FindUserByEmailUseCase;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.port.query.UserAccountQueryRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FindUserByEmailService implements FindUserByEmailUseCase {

    private final UserAccountQueryRepository repository;

    public FindUserByEmailService(UserAccountQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Result> handle(Query query) {
        return repository.findByEmail(new Email(query.email()));
    }
}
