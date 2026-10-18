package com.minewaku.chatter.identityaccess.user.internal.port;

import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import java.time.Instant;

public interface EmailChangeNotifier {

    /** Sends the confirmation token to the new address only. */
    void sendEmailChange(Email newEmail, String rawToken, Instant expiresAt);
}
