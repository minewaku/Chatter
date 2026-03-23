package com.minewaku.chatter.identityaccess.domain.service;

import java.time.Duration;

import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.DeviceInfo;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.Session;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.SessionId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.HashedPassword;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.UniqueStringIdGenerator;

import lombok.NonNull;

public class LoginDomainService {

    private final UniqueStringIdGenerator uniqueStringIdGenerator;

    public LoginDomainService(
                UniqueStringIdGenerator uniqueStringIdGenerator) {

        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
    }


    public Session handle(    
            @NonNull User user,
            @NonNull HashedPassword hashedPassword,
            @NonNull DeviceInfo deviceInfo,
            @NonNull Duration sessionLifespan) {

        user.authenticate(hashedPassword);
        SessionId sessionId = new SessionId(uniqueStringIdGenerator.generate());
        Session session = Session.createNew(sessionId, user.getId(), deviceInfo, sessionLifespan);

        return session;
    }
}
