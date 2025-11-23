package com.minewaku.chatter.application.service.confirmation_token;

import com.minewaku.chatter.domain.port.in.confirmation_token.DeleteConfirmationTokenUseCase;
import com.minewaku.chatter.domain.port.out.repository.ConfirmationTokenRepository;

public class DeleteConfirmationTokenApplicationService implements DeleteConfirmationTokenUseCase {

	private final ConfirmationTokenRepository confirmationTokenRepository;


	public DeleteConfirmationTokenApplicationService(ConfirmationTokenRepository confirmationTokenRepository) {
		this.confirmationTokenRepository = confirmationTokenRepository;
	}
	
	
    @Override
    public Void handle(String token) {
        confirmationTokenRepository.deleteByToken(token);
        return null;
	}
}
