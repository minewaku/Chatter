package com.minewaku.chatter.identityaccess.application.service.command.user.credentials;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.exception.EntityNotFoundException;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.ChangePasswordUseCase;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.Password;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.PasswordHasher;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class ChangePasswordApplicationService implements ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public ChangePasswordApplicationService(
            UserRepository userRepository,
            PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Retry(name = "transientDataAccess")
    @Transactional
    public Void handle(ChangePasswordUseCase.Command command) {
        User user = userRepository.findById(new UserId(command.userId()))
                .orElseThrow(() -> new EntityNotFoundException("User does not exist"));

        boolean passwordChanged = user.changePassword(passwordHasher, new Password(command.password()), new Password(command.newPassword()));
        if (passwordChanged) {
            userRepository.save(user);
        }
        return null;
    }
}
