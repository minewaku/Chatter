package com.minewaku.chatter.identityaccess.application.port.outbound.provider;

import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;

public interface AccessTokenGenerator {
	String generate(UserId userId, Email email);
}
