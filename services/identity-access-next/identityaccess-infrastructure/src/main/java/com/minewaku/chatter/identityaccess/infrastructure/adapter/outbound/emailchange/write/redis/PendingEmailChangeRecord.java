package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.emailchange.write.redis;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stored shape of a pending email change. Times are epoch microseconds. The raw token is never part of it. The
 * confirmed time is stored under {@code verifiedAt} so the shared invalidate script keeps confirmed records.
 */
record PendingEmailChangeRecord(long userId, String email, long createdAt, long expiresAt, Long confirmedAt) {

    Map<String, String> toHash() {
        Map<String, String> hash = new LinkedHashMap<>();
        hash.put("userId", Long.toString(userId));
        hash.put("email", email);
        hash.put("createdAt", Long.toString(createdAt));
        hash.put("expiresAt", Long.toString(expiresAt));
        if (confirmedAt != null) {
            hash.put("verifiedAt", Long.toString(confirmedAt));
        }
        return hash;
    }

    static PendingEmailChangeRecord fromHash(Map<String, String> hash) {
        String confirmed = hash.get("verifiedAt");
        return new PendingEmailChangeRecord(
                Long.parseLong(hash.get("userId")),
                hash.get("email"),
                Long.parseLong(hash.get("createdAt")),
                Long.parseLong(hash.get("expiresAt")),
                confirmed == null ? null : Long.valueOf(confirmed));
    }
}
