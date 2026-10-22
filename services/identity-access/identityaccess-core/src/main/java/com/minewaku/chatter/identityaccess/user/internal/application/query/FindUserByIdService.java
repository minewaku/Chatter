package com.minewaku.chatter.identityaccess.user.internal.application.query;

import com.minewaku.chatter.identityaccess.user.api.query.FindUserByIdUseCase;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.port.query.UserAccountQueryRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindUserByIdService implements FindUserByIdUseCase {

    private final UserAccountQueryRepository repository;

    @Override
    public Optional<Result> handle(Query query) {
        return repository.findById(new UserId(query.userId()));
    }
}
