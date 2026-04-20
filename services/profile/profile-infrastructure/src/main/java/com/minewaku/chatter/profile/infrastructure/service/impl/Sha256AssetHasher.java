package com.minewaku.chatter.profile.infrastructure.service.impl;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.profile.application.port.outbound.provider.AssetHasher;
import com.minewaku.chatter.profile.domain.sharedkernel.exception.core.AppException;

@Service
public class Sha256AssetHasher implements AssetHasher {

    @Override
    public String hash(InputStream inputStream) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            DigestInputStream digestStream = new DigestInputStream(inputStream, md); 
            byte[] digest = md.digest();
            String hashString = HexFormat.of().formatHex(digest);
            return hashString;
        } catch (NoSuchAlgorithmException e) {
            throw new AppException("Failed to compute asset hash", e);
        }
    }
    
}
