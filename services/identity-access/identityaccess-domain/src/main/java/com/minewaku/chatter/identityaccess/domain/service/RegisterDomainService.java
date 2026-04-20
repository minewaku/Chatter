package com.minewaku.chatter.identityaccess.domain.service;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.identityaccess.domain.aggregate.user.exception.RegisterExistDisableUserException;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.exception.UserAlreadyExistException;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Birthday;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Username;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.HashedPassword;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.Password;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.PasswordHasher;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.TimeBasedIdGenerator;

import lombok.NonNull;

@Service
public class RegisterDomainService {
    
    private final UserRepository userRepository;
    private final TimeBasedIdGenerator timeBasedIdGenerator;
    private final PasswordHasher passwordHasher;

    public RegisterDomainService(
                UserRepository userRepository,
                TimeBasedIdGenerator timeBasedIdGenerator,
                PasswordHasher passwordHasher) {

        this.userRepository = userRepository;
        this.timeBasedIdGenerator = timeBasedIdGenerator;
        this.passwordHasher = passwordHasher;
    }

    public User handle(
            @NonNull Email email, 
            @NonNull Username username, 
            @NonNull Birthday birthday, 
            @NonNull Password password) {

        userRepository.findByEmail(email).ifPresent(existingUser -> {
            if (existingUser.getEnablement().isEnabled()) {
                throw new UserAlreadyExistException("User already active");
            }
            throw new RegisterExistDisableUserException("User pending activation");
        });

        UserId userId = new UserId(timeBasedIdGenerator.generate());
		HashedPassword hashedPassword = passwordHasher.hash(password);

		User user = User.register(
				userId,
				email,
				username,
				birthday,
				hashedPassword);

        return user;
    }
}
