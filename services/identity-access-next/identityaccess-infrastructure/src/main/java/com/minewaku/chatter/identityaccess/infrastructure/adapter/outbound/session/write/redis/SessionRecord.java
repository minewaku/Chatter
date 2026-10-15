package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.session.write.redis;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stored shape of a session hash. Times are epoch microseconds. Only the digest of the refresh credential is kept.
 * The session id is the key suffix, so it is not repeated inside the hash.
 */
record SessionRecord(
        String sessionId,
        long userId,
        String deviceInfo,
        long issuedAt,
        long expiresAt,
        Long lastUsedAt,
        Long revokedAt,
        String tokenDigest) {

    Map<String, String> toHash() {
        Map<String, String> hash = new LinkedHashMap<>();
        hash.put("userId", Long.toString(userId));
        if (deviceInfo != null) {
            hash.put("deviceInfo", deviceInfo);
        }
        hash.put("issuedAt", Long.toString(issuedAt));
        hash.put("expiresAt", Long.toString(expiresAt));
        if (lastUsedAt != null) {
            hash.put("lastUsedAt", Long.toString(lastUsedAt));
        }
        if (revokedAt != null) {
            hash.put("revokedAt", Long.toString(revokedAt));
        }
        hash.put("tokenDigest", tokenDigest);
        return hash;
    }

    static SessionRecord fromHash(String sessionId, Map<String, String> hash) {
        return new SessionRecord(
                sessionId,
                Long.parseLong(hash.get("userId")),
                hash.get("deviceInfo"),
                Long.parseLong(hash.get("issuedAt")),
                Long.parseLong(hash.get("expiresAt")),
                optionalLong(hash.get("lastUsedAt")),
                optionalLong(hash.get("revokedAt")),
                hash.get("tokenDigest"));
    }

    private static Long optionalLong(String value) {
        return value == null ? null : Long.valueOf(value);
    }
}
