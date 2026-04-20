package com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.repository;

import java.util.Optional;

import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.model.ConfirmationToken;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;

public interface ConfirmationTokenRepository {
	ConfirmationToken save(ConfirmationToken confirmationToken);
	Optional<ConfirmationToken> findByToken(String token);
	void deleteByEmail(Email email);
}
