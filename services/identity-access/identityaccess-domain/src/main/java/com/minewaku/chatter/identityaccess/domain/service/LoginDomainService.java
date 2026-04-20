package com.minewaku.chatter.identityaccess.domain.service;

import java.time.Duration;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.DeviceInfo;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.Session;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.SessionId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.Password;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.PasswordHasher;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.UniqueStringIdGenerator;

import lombok.NonNull;

@Service
public class LoginDomainService{

    private final UniqueStringIdGenerator uniqueStringIdGenerator;

    public LoginDomainService(
                UniqueStringIdGenerator uniqueStringIdGenerator) {

        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
    }


    public Session handle(
            @NonNull PasswordHasher passwordHasher,
            @NonNull User user,
            @NonNull Password password,
            @NonNull DeviceInfo deviceInfo,
            Duration sessionLifespan) {

        user.authenticate(passwordHasher, password);
        SessionId sessionId = new SessionId(uniqueStringIdGenerator.generate());
        Session session = Session.createNew(sessionId, user.getId(), deviceInfo, sessionLifespan);

        return session;
    }
}
