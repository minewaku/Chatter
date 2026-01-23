package com.minewaku.chatter.domain.service.auth;

import com.minewaku.chatter.domain.exception.BusinessRuleViolationException;
import com.minewaku.chatter.domain.exception.RegisterExistDisableUserException;
import com.minewaku.chatter.domain.model.User;

public class CheckRegisterUserDomainService {

    public CheckRegisterUserDomainService() {

    }

    public void handle(User user) {
        user.checkForSoftDeleted();
        user.checkForLocked();
        
        if (user.isEnabled()) {
            throw new BusinessRuleViolationException("User with this email already exists");
        } else {
            throw new RegisterExistDisableUserException(
                    "This email is already registered without verification. Please verify your email first.");
        }
    }
}
