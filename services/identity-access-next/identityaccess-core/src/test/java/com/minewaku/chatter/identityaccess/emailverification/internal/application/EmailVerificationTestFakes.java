package com.minewaku.chatter.identityaccess.emailverification.internal.application;

import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.VerificationPurpose;
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

public final class EmailVerificationTestFakes {
    private EmailVerificationTestFakes() {}

    /** Records are keyed by raw token; a lookup also requires the purpose to match, as the real repository does. */
    public static final class Repository implements EmailVerificationRepository {
        public final Map<String, EmailVerification> records = new LinkedHashMap<>();
        public final List<String> calls = new ArrayList<>();
        public RuntimeException failure;

        @Override
        public void save(EmailVerification verification) {
            calls.add("save");
            if (failure != null) throw failure;
            records.put(verification.rawToken(), verification);
        }

        @Override
        public Optional<EmailVerification> findByPresentedToken(VerificationPurpose purpose, String token) {
            calls.add("find");
            return Optional.ofNullable(records.get(token)).filter(found -> found.purpose() == purpose);
        }

        @Override
        public void invalidateForUser(VerificationPurpose purpose, UserId userId) {
            calls.add("invalidate");
            records.entrySet()
                    .removeIf(entry -> entry.getValue().purpose() == purpose
                            && entry.getValue().userId().equals(userId)
                            && !entry.getValue().isVerified());
        }

        /** The user's unverified proof of this purpose, if any. */
        public Optional<EmailVerification> outstandingFor(VerificationPurpose purpose, long userId) {
            return records.values().stream()
                    .filter(found ->
                            found.purpose() == purpose && found.userId().value() == userId && !found.isVerified())
                    .findFirst();
        }
    }

    public static final class Generator implements UniqueStringIdGenerator {
        public final List<String> values = new ArrayList<>(List.of("token-1", "token-2", "token-3"));
        public int calls;
        public RuntimeException failure;

        @Override
        public String nextId() {
            calls++;
            if (failure != null) throw failure;
            return values.removeFirst();
        }
    }

    public static final class Notifier implements VerificationNotifier {
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
