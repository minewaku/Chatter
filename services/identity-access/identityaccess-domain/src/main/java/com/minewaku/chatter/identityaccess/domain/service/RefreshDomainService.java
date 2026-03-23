package com.minewaku.chatter.identityaccess.domain.service;

import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.Session;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.BusinessRuleViolationException;

import lombok.NonNull;

public class RefreshDomainService {
    
    public RefreshDomainService() {

    }

    public Session handle(@NonNull User user, @NonNull Session session, int generation) {

        if(user.getId() != session.getUserId()) {
            throw new BusinessRuleViolationException("Session does not belong to the user");
        }

        user.isAccessible();
        session.refresh(generation);

        return session;
    }
}
