package com.minewaku.chatter.identityaccess.domain.aggregate.user.repository;

import java.util.Optional;

import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;

public interface UserRepository {
    void save(User user);
    void delete(User user);
    void deleteById(UserId userId);
    Optional<User> findById(UserId userId);
    Optional<User> findByEmail(Email email);
}
