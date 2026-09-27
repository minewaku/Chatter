package com.minewaku.chatter.identityaccess.user.internal.application.query;

import com.minewaku.chatter.identityaccess.user.api.query.FindUserByIdUseCase;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccountView;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountViewRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FindUserByIdService implements FindUserByIdUseCase {

    private final UserAccountViewRepository repository;

    public FindUserByIdService(UserAccountViewRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Result> handle(Query query) {
        return repository.findById(new UserId(query.userId())).map(this::toResult);
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
