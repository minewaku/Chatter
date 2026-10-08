package com.minewaku.chatter.identityaccess.user.internal.application;

import com.minewaku.chatter.identityaccess.user.api.query.FindUserByEmailUseCase;
import com.minewaku.chatter.identityaccess.user.api.query.FindUserByIdUseCase;
import com.minewaku.chatter.identityaccess.user.internal.exception.DuplicateUserAccountException;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.PasswordHash;
import com.minewaku.chatter.identityaccess.user.internal.model.PlainPassword;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.model.Username;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import com.minewaku.chatter.identityaccess.user.internal.port.UserIdGenerator;
import com.minewaku.chatter.identityaccess.user.internal.port.query.UserAccountQueryRepository;
import com.minewaku.chatter.identityaccess.user.internal.port.repository.UserAccountRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

final class UserAccountTestFakes {

    private UserAccountTestFakes() {}

    static final class InMemoryRepository implements UserAccountRepository {
        private final Map<UserId, UserAccount> accounts = new LinkedHashMap<>();
        int findByIdCalls;
        int findByEmailCalls;
        int saveCalls;
        boolean duplicateOnNextSave;
        UserAccount accountToExposeAfterDuplicate;

        void add(UserAccount account) {
            accounts.put(account.id(), account);
        }

        @Override
        public Optional<UserAccount> findById(UserId userId) {
            findByIdCalls++;
            return Optional.ofNullable(accounts.get(userId));
        }

        @Override
        public Optional<UserAccount> findByEmail(Email email) {
            findByEmailCalls++;
            return accounts.values().stream()
                    .filter(account -> account.email().equals(email))
                    .findFirst();
        }

        @Override
        public Optional<UserAccount> findByUsername(Username username) {
            return accounts.values().stream()
                    .filter(account -> account.username().equals(username))
                    .findFirst();
        }

        @Override
        public UserAccount save(UserAccount account) {
            saveCalls++;
            if (duplicateOnNextSave) {
                duplicateOnNextSave = false;
                add(accountToExposeAfterDuplicate);
                throw new DuplicateUserAccountException("duplicate", null);
            }
            add(account);
            return account;
        }
    }

    static final class InMemoryQueryRepository implements UserAccountQueryRepository {
        private final Map<UserId, FindUserByIdUseCase.Result> accountsById = new LinkedHashMap<>();
        private final Map<Email, FindUserByEmailUseCase.Result> accountsByEmail = new LinkedHashMap<>();
        int findByIdCalls;
        int findByEmailCalls;

        void addById(FindUserByIdUseCase.Result account) {
            accountsById.put(new UserId(account.userId()), account);
        }

        void addByEmail(FindUserByEmailUseCase.Result account) {
            accountsByEmail.put(new Email(account.email()), account);
        }

        @Override
        public Optional<FindUserByIdUseCase.Result> findById(UserId userId) {
            findByIdCalls++;
            return Optional.ofNullable(accountsById.get(userId));
        }

        @Override
        public Optional<FindUserByEmailUseCase.Result> findByEmail(Email email) {
            findByEmailCalls++;
            return Optional.ofNullable(accountsByEmail.get(email));
        }
    }

    static final class DeterministicPasswordHasher implements PasswordHasher {
        @Override
        public PasswordHash hash(PlainPassword password) {
            return new PasswordHash("test", String.valueOf(password.characters()), new byte[] {1});
        }

        @Override
        public boolean matches(PlainPassword password, PasswordHash passwordHash) {
            return passwordHash.hash().equals(String.valueOf(password.characters()));
        }
    }

    static final class SequentialUserIdGenerator implements UserIdGenerator {
        private long nextValue;
        int calls;

        SequentialUserIdGenerator(long initialValue) {
            nextValue = initialValue;
        }

        @Override
        public UserId nextId() {
            calls++;
            return new UserId(nextValue++);
        }
    }
}
