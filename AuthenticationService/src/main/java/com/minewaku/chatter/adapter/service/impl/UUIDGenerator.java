package com.minewaku.chatter.adapter.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.domain.port.out.service.ConfirmationTokenGenerator;
import com.minewaku.chatter.domain.port.out.service.RefreshTokenGenerator;

@Service
public class UUIDGenerator implements ConfirmationTokenGenerator, RefreshTokenGenerator {
	
	@Override
	public String generate() {
		return UUID.randomUUID().toString();
	}
}
