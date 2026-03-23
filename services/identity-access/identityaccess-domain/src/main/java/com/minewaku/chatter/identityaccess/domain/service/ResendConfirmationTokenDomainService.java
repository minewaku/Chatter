package com.minewaku.chatter.identityaccess.domain.service;

import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.model.ConfirmationToken;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.UniqueStringIdGenerator;

public class ResendConfirmationTokenDomainService {

    private final UniqueStringIdGenerator uniqueStringIdGenerator;
    
    public ResendConfirmationTokenDomainService(
                UniqueStringIdGenerator uniqueStringIdGenerator) {
                    
        this.uniqueStringIdGenerator = uniqueStringIdGenerator;
    }

    public ConfirmationToken handle(User user) {
        user.isAccessible();

        String token = uniqueStringIdGenerator.generate();
        ConfirmationToken confirmationToken = ConfirmationToken.createNew(
            token,
            user.getId(),
            user.getEmail(),
            null);

        return confirmationToken;
    }
}
