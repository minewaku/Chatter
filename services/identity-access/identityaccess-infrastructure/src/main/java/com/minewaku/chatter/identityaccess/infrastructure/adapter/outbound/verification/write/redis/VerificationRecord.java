package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.verification.write.redis;

import java.util.LinkedHashMap;
import java.util.Map;

/** Stored shape of a verification token. Times are epoch microseconds. The raw token is never part of it. */
record VerificationRecord(long userId, String email, long createdAt, long expiresAt, Long verifiedAt) {

    Map<String, String> toHash() {
        Map<String, String> hash = new LinkedHashMap<>();
        hash.put("userId", Long.toString(userId));
        hash.put("email", email);
        hash.put("createdAt", Long.toString(createdAt));
        hash.put("expiresAt", Long.toString(expiresAt));
        if (verifiedAt != null) {
            hash.put("verifiedAt", Long.toString(verifiedAt));
        }
        return hash;
    }

    static VerificationRecord fromHash(Map<String, String> hash) {
        String verified = hash.get("verifiedAt");
        return new VerificationRecord(
                Long.parseLong(hash.get("userId")),
                hash.get("email"),
                Long.parseLong(hash.get("createdAt")),
                Long.parseLong(hash.get("expiresAt")),
                verified == null ? null : Long.valueOf(verified));
    }
}
