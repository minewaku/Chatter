package com.minewaku.chatter.identityaccess.domain.service;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.model.ConfirmationToken;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.model.ConfirmationTokenId;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.BusinessRuleViolationException;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.UniqueStringIdGenerator;

@Service
public class ResendConfirmationTokenDomainService {

    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    
    public ResendConfirmationTokenDomainService(
                UniqueStringIdGenerator uniqueStringIdGenerator) {
                    
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
    }

    public ConfirmationToken handle(User user) {

        if(!user.isUnverified()) {
            throw new BusinessRuleViolationException("User is already verified");
        }

        String token = uniqueStringIdGenerator.generate();
        ConfirmationTokenId id = new ConfirmationTokenId(token);
        return ConfirmationToken.createNew(
            id,
            user.getId(),
            user.getEmail(),
            null);
    }
}
