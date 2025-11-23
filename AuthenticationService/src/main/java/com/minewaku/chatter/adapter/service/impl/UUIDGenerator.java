package com.minewaku.chatter.adapter.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.domain.port.out.service.KeyGenerator;

@Service
public class UUIDGenerator implements KeyGenerator {
	
	@Override
	public String generate() {
		return UUID.randomUUID().toString();
	}
}
