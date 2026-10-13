package com.minewaku.chatter.identityaccess.emailverification.internal.application;

import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.emailverification.internal.port.VerificationNotifier;
import com.minewaku.chatter.identityaccess.emailverification.internal.port.repository.EmailVerificationRepository;
import com.minewaku.chatter.identityaccess.shared.port.UniqueStringIdGenerator;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class EmailVerificationTestFakes {
    private EmailVerificationTestFakes() {}

    static final class Repository implements EmailVerificationRepository {
        final Map<String, EmailVerification> records = new LinkedHashMap<>();
        final List<String> calls = new ArrayList<>();
        RuntimeException failure;

        @Override
        public void save(EmailVerification verification) {
            calls.add("save");
            if (failure != null) throw failure;
            records.put(verification.rawToken(), verification);
        }

        @Override
        public Optional<EmailVerification> findByPresentedToken(String token) {
            calls.add("find");
            return Optional.ofNullable(records.get(token));
        }

        @Override
        public void invalidateForUser(UserId userId) {
            calls.add("invalidate");
            records.entrySet()
                    .removeIf(entry -> entry.getValue().userId().equals(userId)
                            && !entry.getValue().isVerified());
        }
    }

    static final class Generator implements UniqueStringIdGenerator {
        final List<String> values = new ArrayList<>(List.of("token-1", "token-2", "token-3"));
        int calls;
        RuntimeException failure;

        @Override
        public String nextId() {
            calls++;
            if (failure != null) throw failure;
            return values.removeFirst();
        }
    }

    static final class Notifier implements VerificationNotifier {
        record Delivery(Email email, String token, Instant expiresAt) {}

        final List<Delivery> deliveries = new ArrayList<>();
        final List<String> calls = new ArrayList<>();
        RuntimeException failure;

        @Override
        public void sendVerification(Email email, String rawToken, Instant expiresAt) {
            calls.add("notify");
            if (failure != null) throw failure;
            deliveries.add(new Delivery(email, rawToken, expiresAt));
        }
    }
}
