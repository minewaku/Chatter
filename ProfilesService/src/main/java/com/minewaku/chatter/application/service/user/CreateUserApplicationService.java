package com.minewaku.chatter.application.service.user;

import com.minewaku.chatter.domain.command.profile.CreateUserCommand;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.user.CreateUserUseCase;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;

public class CreateUserApplicationService implements CreateUserUseCase {

    private final UserRepository userRepository;

    public CreateUserApplicationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public Void handle(CreateUserCommand command) {
        User user = User.createNew(command.getId(),
                command.getEmail(),
                command.getUsername(),
                command.getBirthday(),
                command.isEnabled(),
                command.isLocked(),
                command.isDeleted(),
                command.getDeletedAt(),
                command.getAuditMetadata()
                );

        userRepository.save(user);
        return null;
    }
}
