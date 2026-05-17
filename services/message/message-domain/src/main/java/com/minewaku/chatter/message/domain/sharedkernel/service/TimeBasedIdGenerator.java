package com.minewaku.chatter.message.domain.sharedkernel.service;

public interface TimeBasedIdGenerator {
	long generate();
	long toTimeStamp(long id);
}
