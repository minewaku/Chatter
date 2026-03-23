package com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.repository;

import java.util.Optional;

import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.model.ConfirmationToken;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;

public interface ConfirmationTokenRepository {
	Optional<ConfirmationToken> findByToken(String token);
	void deleteByEmail(Email email);
	void deleteByToken(String token);
	ConfirmationToken save(ConfirmationToken confirmationToken);
	void confirmEmail(ConfirmationToken confirmationToken);
}
