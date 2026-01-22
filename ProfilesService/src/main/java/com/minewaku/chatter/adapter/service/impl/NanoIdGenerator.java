package com.minewaku.chatter.adapter.service.impl;

import org.springframework.stereotype.Service;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.minewaku.chatter.domain.port.out.service.FileStorageKeyGenerator;

@Service
public class NanoIdGenerator implements FileStorageKeyGenerator {
    @Override
    public String generate() {
        return NanoIdUtils.randomNanoId();
    }
}
