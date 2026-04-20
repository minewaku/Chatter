package com.minewaku.chatter.identityaccess.domain.service;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.Session;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.BusinessRuleViolationException;

import lombok.NonNull;

@Service
public class RefreshDomainService {
    
    public RefreshDomainService() {

    }

    public Session handle(@NonNull User user, @NonNull Session session, int generation) {
        user.isAccessible();

        if (!user.getId().equals(session.getUserId())) {
            throw new BusinessRuleViolationException("Session does not belong to the user" + "UserId: " + user.getId() + ", SessionUserId: " + session.getUserId());
        }

        user.isAccessible();
        if(session.refresh(generation)) {
            return session;
        }

        return null;
    }
}
