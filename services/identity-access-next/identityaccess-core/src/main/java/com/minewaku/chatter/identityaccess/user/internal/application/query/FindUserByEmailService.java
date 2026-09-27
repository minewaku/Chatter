package com.minewaku.chatter.identityaccess.user.internal.application.query;

import com.minewaku.chatter.identityaccess.user.api.query.FindUserByEmailUseCase;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccountView;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountViewRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FindUserByEmailService implements FindUserByEmailUseCase {

    private final UserAccountViewRepository repository;

    public FindUserByEmailService(UserAccountViewRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Result> handle(Query query) {
        return repository.findByEmail(new Email(query.email())).map(this::toResult);
    }

    private Result toResult(UserAccountView account) {
        return new Result(
                account.userId(),
                account.email(),
                account.username(),
                account.birthday(),
                account.status(),
                account.accessible(),
                account.deletedAt(),
                account.createdAt(),
                account.updatedAt());
    }
}
