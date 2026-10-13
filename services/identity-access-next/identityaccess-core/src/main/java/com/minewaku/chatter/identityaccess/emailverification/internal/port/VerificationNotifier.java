package com.minewaku.chatter.identityaccess.emailverification.internal.port;

import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import java.time.Instant;

public interface VerificationNotifier {
    void sendVerification(Email email, String rawToken, Instant expiresAt);
}
