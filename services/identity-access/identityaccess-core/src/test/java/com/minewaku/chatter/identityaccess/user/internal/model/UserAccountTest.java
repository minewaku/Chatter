package com.minewaku.chatter.identityaccess.user.internal.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.user.api.exception.InvalidAccountStateTransitionException;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidCredentialsException;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class UserAccountTest {

    private static final PlainPassword PASSWORD = new PlainPassword("Password1!");
    private static final PlainPassword INCORRECT_PASSWORD = new PlainPassword("Incorrect2@");
    private static final String PRESENTED_PASSWORD = "Password1!";
    private static final String PRESENTED_INCORRECT_PASSWORD = "Incorrect2@";
    private static final String PRESENTED_WEAK_PASSWORD = "weak";
    private static final Instant ORIGINAL_UPDATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final PasswordHasher PASSWORD_HASHER = new TestPasswordHasher();

    private UserAccount account(AccountStatus status) {
        return UserAccount.reconstitute(
                new UserId(42),
                new Email("Person@Example.com"),
                new Username("person_42"),
                new Birthday(LocalDate.of(1990, 1, 1)),
                status,
                null,
                PASSWORD_HASHER.hash(PASSWORD),
                ORIGINAL_UPDATED_AT,
                ORIGINAL_UPDATED_AT,
                0L);
    }

    @Test
    void registration_is_pending_and_inaccessible() {
        UserAccount account = UserAccount.register(
                new UserId(42),
                new Email("Person@Example.com"),
                new Username("person_42"),
                new Birthday(LocalDate.of(1990, 1, 1)),
                PASSWORD_HASHER.hash(PASSWORD));

        assertEquals(AccountStatus.PENDING_VERIFICATION, account.status());
        assertFalse(account.isAccessible());
        assertEquals("person@example.com", account.email().value());
    }

    @Test
    void allows_every_valid_transition_and_target_state_idempotency() {
        UserAccount account = account(AccountStatus.PENDING_VERIFICATION);

        assertTrue(account.activateAfterVerification());
        assertFalse(account.activateAfterVerification());
        assertTrue(account.lock());
        assertFalse(account.lock());
        assertTrue(account.unlock());
        assertFalse(account.unlock());
        assertTrue(account.suspend());
        assertFalse(account.suspend());
        assertTrue(account.reinstate());
        assertFalse(account.reinstate());
        assertEquals(AccountStatus.ACTIVE, account.status());
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void activate_after_verification_follows_the_status_policy(AccountStatus source) {
        assertTransitionPolicy(
                source,
                AccountStatus.PENDING_VERIFICATION,
                AccountStatus.ACTIVE,
                UserAccount::activateAfterVerification);
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void lock_follows_the_status_policy(AccountStatus source) {
        assertTransitionPolicy(source, AccountStatus.ACTIVE, AccountStatus.LOCKED, UserAccount::lock);
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void unlock_follows_the_status_policy(AccountStatus source) {
        assertTransitionPolicy(source, AccountStatus.LOCKED, AccountStatus.ACTIVE, UserAccount::unlock);
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void suspend_follows_the_status_policy(AccountStatus source) {
        assertTransitionPolicy(source, AccountStatus.ACTIVE, AccountStatus.SUSPENDED, UserAccount::suspend);
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void reinstate_follows_the_status_policy(AccountStatus source) {
        assertTransitionPolicy(source, AccountStatus.SUSPENDED, AccountStatus.ACTIVE, UserAccount::reinstate);
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void soft_delete_follows_the_status_policy(AccountStatus source) {
        UserAccount account = account(source);

        if (source == AccountStatus.DELETED) {
            assertFalse(account.softDelete());
            assertEquals(ORIGINAL_UPDATED_AT, account.updatedAt());
        } else {
            assertTrue(account.softDelete());
            assertEquals(AccountStatus.DELETED, account.status());
            assertNotEquals(ORIGINAL_UPDATED_AT, account.updatedAt());
        }
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = "DELETED", mode = EnumSource.Mode.EXCLUDE)
    void close_with_the_correct_password_deletes_the_account_from_every_non_deleted_status(AccountStatus source) {
        UserAccount account = account(source);

        assertTrue(account.close(PRESENTED_PASSWORD, PASSWORD_HASHER));

        assertEquals(AccountStatus.DELETED, account.status());
        assertEquals("deleted-42@deleted.invalid", account.email().value());
        assertEquals("deleted_42", account.username().value());
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void close_with_a_wrong_password_changes_nothing(AccountStatus source) {
        UserAccount account = account(source);

        assertThrows(InvalidCredentialsException.class, () -> account.close(PRESENTED_INCORRECT_PASSWORD, PASSWORD_HASHER));

        assertEquals(source, account.status());
        assertEquals("person@example.com", account.email().value());
        assertEquals("person_42", account.username().value());
        assertEquals(ORIGINAL_UPDATED_AT, account.updatedAt());
    }

    @Test
    void close_on_an_already_closed_account_fails_the_password_check() {
        UserAccount account = account(AccountStatus.ACTIVE);
        assertTrue(account.close(PRESENTED_PASSWORD, PASSWORD_HASHER));

        assertThrows(InvalidCredentialsException.class, () -> account.close(PRESENTED_PASSWORD, PASSWORD_HASHER));
    }

    @Test
    void rejects_cross_state_operations() {
        assertThrows(InvalidAccountStateTransitionException.class, () -> account(AccountStatus.PENDING_VERIFICATION)
                .unlock());
        assertThrows(InvalidAccountStateTransitionException.class, () -> account(AccountStatus.PENDING_VERIFICATION)
                .reinstate());
        assertThrows(InvalidAccountStateTransitionException.class, () -> account(AccountStatus.LOCKED)
                .suspend());
        assertThrows(InvalidAccountStateTransitionException.class, () -> account(AccountStatus.SUSPENDED)
                .lock());
    }

    @Test
    void deletion_is_terminal_anonymous_and_idempotent_from_every_non_deleted_status() {
        for (AccountStatus status : Arrays.asList(
                AccountStatus.PENDING_VERIFICATION,
                AccountStatus.ACTIVE,
                AccountStatus.LOCKED,
                AccountStatus.SUSPENDED)) {
            UserAccount account = account(status);

            assertTrue(account.softDelete());
            assertEquals(AccountStatus.DELETED, account.status());
            assertNotNull(account.deletedAt());
            assertEquals("deleted-42@deleted.invalid", account.email().value());
            assertEquals("deleted_42", account.username().value());
            assertNotEquals(PASSWORD_HASHER.hash(PASSWORD), account.passwordHash());
            assertFalse(account.softDelete());
        }
    }

    @Test
    void deletion_rejects_all_later_state_and_credential_or_identity_changes() {
        UserAccount account = account(AccountStatus.ACTIVE);
        account.softDelete();

        assertThrows(InvalidAccountStateTransitionException.class, account::activateAfterVerification);
        assertThrows(InvalidAccountStateTransitionException.class, account::lock);
        assertThrows(InvalidAccountStateTransitionException.class, account::unlock);
        assertThrows(InvalidAccountStateTransitionException.class, account::suspend);
        assertThrows(InvalidAccountStateTransitionException.class, account::reinstate);
        assertThrows(
                InvalidAccountStateTransitionException.class,
                () -> account.changePassword(PRESENTED_PASSWORD, new PlainPassword("NewPassword2@"), PASSWORD_HASHER));
        assertThrows(
                InvalidAccountStateTransitionException.class, () -> account.changeUsername(new Username("new_person")));
        assertThrows(
                InvalidAccountStateTransitionException.class,
                () -> account.authorizeEmailChange(PRESENTED_PASSWORD, new Email("new@example.com"), PASSWORD_HASHER));
        assertThrows(
                InvalidAccountStateTransitionException.class,
                () -> account.applyVerifiedEmail(new Email("new@example.com")));
    }

    @Test
    void password_changes_require_accessible_correct_credentials_and_a_new_value() {
        UserAccount account = account(AccountStatus.ACTIVE);
        PlainPassword newPassword = new PlainPassword("NewPassword2@");

        assertTrue(account.changePassword(PRESENTED_PASSWORD, newPassword, PASSWORD_HASHER));
        assertTrue(PASSWORD_HASHER.matches(newPassword, account.passwordHash()));
        assertFalse(PASSWORD_HASHER.matches(PASSWORD, account.passwordHash()));
        assertFalse(account.changePassword("NewPassword2@", newPassword, PASSWORD_HASHER));
        assertThrows(
                InvalidCredentialsException.class,
                () -> account.changePassword(
                        PRESENTED_INCORRECT_PASSWORD, new PlainPassword("OtherPassword3#"), PASSWORD_HASHER));
        assertThrows(InvalidAccountStateTransitionException.class, () -> account(AccountStatus.LOCKED)
                .changePassword(PRESENTED_PASSWORD, new PlainPassword("OtherPassword3#"), PASSWORD_HASHER));
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void password_change_rejects_every_inaccessible_status_before_matching_credentials(AccountStatus status) {
        UserAccount account = account(status);

        assertThrows(
                InvalidAccountStateTransitionException.class,
                () -> account.changePassword(PRESENTED_INCORRECT_PASSWORD, new PlainPassword("NewPassword2@"), PASSWORD_HASHER));
        assertEquals(ORIGINAL_UPDATED_AT, account.updatedAt());
        assertTrue(PASSWORD_HASHER.matches(PASSWORD, account.passwordHash()));
    }

    @Test
    void username_changes_need_no_password_but_an_accessible_account_and_a_new_value() {
        UserAccount account = account(AccountStatus.ACTIVE);

        assertTrue(account.changeUsername(new Username("new_person")));
        assertEquals("new_person", account.username().value());
        assertNotEquals(ORIGINAL_UPDATED_AT, account.updatedAt());
        assertFalse(account.changeUsername(new Username("new_person")));
    }

    @Test
    void an_unchanged_username_leaves_the_account_untouched() {
        UserAccount account = account(AccountStatus.ACTIVE);

        assertFalse(account.changeUsername(new Username("person_42")));
        assertEquals(ORIGINAL_UPDATED_AT, account.updatedAt());
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void username_change_rejects_every_inaccessible_status(AccountStatus status) {
        UserAccount account = account(status);

        assertThrows(
                InvalidAccountStateTransitionException.class, () -> account.changeUsername(new Username("new_person")));
        assertEquals(ORIGINAL_UPDATED_AT, account.updatedAt());
        assertEquals("person_42", account.username().value());
    }

    @Test
    void email_change_authorization_needs_the_password_and_a_new_normalized_value_and_changes_nothing() {
        UserAccount account = account(AccountStatus.ACTIVE);

        assertFalse(account.authorizeEmailChange(PRESENTED_PASSWORD, new Email(" PERSON@example.com "), PASSWORD_HASHER));
        assertTrue(account.authorizeEmailChange(PRESENTED_PASSWORD, new Email("New@Example.com"), PASSWORD_HASHER));
        assertThrows(
                InvalidCredentialsException.class,
                () -> account.authorizeEmailChange(PRESENTED_INCORRECT_PASSWORD, new Email("other@example.com"), PASSWORD_HASHER));

        assertEquals("person@example.com", account.email().value());
        assertEquals(AccountStatus.ACTIVE, account.status());
        assertEquals(ORIGINAL_UPDATED_AT, account.updatedAt());
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void email_change_authorization_rejects_every_inaccessible_status_before_matching_credentials(
            AccountStatus status) {
        UserAccount account = account(status);

        assertThrows(
                InvalidAccountStateTransitionException.class,
                () -> account.authorizeEmailChange(PRESENTED_INCORRECT_PASSWORD, new Email("new@example.com"), PASSWORD_HASHER));
        assertEquals("person@example.com", account.email().value());
    }

    @Test
    void a_verified_email_replaces_the_current_one_and_keeps_the_account_active() {
        UserAccount account = account(AccountStatus.ACTIVE);

        account.applyVerifiedEmail(new Email("New@Example.com"));

        assertEquals("new@example.com", account.email().value());
        assertEquals(AccountStatus.ACTIVE, account.status());
        assertNotEquals(ORIGINAL_UPDATED_AT, account.updatedAt());
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void a_verified_email_is_not_applied_to_an_inaccessible_account(AccountStatus status) {
        UserAccount account = account(status);

        assertThrows(
                InvalidAccountStateTransitionException.class,
                () -> account.applyVerifiedEmail(new Email("new@example.com")));
        assertEquals("person@example.com", account.email().value());
        assertEquals(ORIGINAL_UPDATED_AT, account.updatedAt());
    }

    @Test
    void value_objects_reject_invalid_values() {
        assertThrows(IllegalArgumentException.class, () -> new UserId(0));
        assertThrows(IllegalArgumentException.class, () -> new Email("not-an-email"));
        assertThrows(IllegalArgumentException.class, () -> new Username(".bad"));
        assertThrows(IllegalArgumentException.class, () -> new PlainPassword("password"));
    }

    private void assertTransitionPolicy(
            AccountStatus source, AccountStatus expected, AccountStatus target, AccountOperation operation) {
        UserAccount account = account(source);

        if (source == target) {
            assertFalse(operation.apply(account));
            assertEquals(target, account.status());
            assertEquals(ORIGINAL_UPDATED_AT, account.updatedAt());
        } else if (source == expected) {
            assertTrue(operation.apply(account));
            assertEquals(target, account.status());
            assertNotEquals(ORIGINAL_UPDATED_AT, account.updatedAt());
        } else {
            assertThrows(InvalidAccountStateTransitionException.class, () -> operation.apply(account));
            assertEquals(source, account.status());
            assertEquals(ORIGINAL_UPDATED_AT, account.updatedAt());
        }
    }

    @FunctionalInterface
    private interface AccountOperation {
        boolean apply(UserAccount account);
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void a_non_active_account_is_reported_as_such_whatever_password_is_presented_and_the_password_is_never_compared(
            AccountStatus status) {
        for (String presented :
                new String[] {PRESENTED_PASSWORD, PRESENTED_INCORRECT_PASSWORD, PRESENTED_WEAK_PASSWORD, null}) {
            CountingHasher hasher = new CountingHasher();
            UserAccount account = account(status);

            assertThrows(
                    InvalidAccountStateTransitionException.class, () -> account.confirmCredentials(presented, hasher));
            assertThrows(
                    InvalidAccountStateTransitionException.class,
                    () -> account.changePassword(presented, new PlainPassword("NewPassword2@"), hasher));
            assertThrows(
                    InvalidAccountStateTransitionException.class,
                    () -> account.authorizeEmailChange(presented, new Email("new@example.com"), hasher));
            assertEquals(0, hasher.matchCalls);
        }
    }

    @Test
    void an_active_account_treats_a_wrong_or_policy_failing_password_as_a_wrong_password() {
        UserAccount account = account(AccountStatus.ACTIVE);

        for (String presented : new String[] {PRESENTED_INCORRECT_PASSWORD, PRESENTED_WEAK_PASSWORD, "", null}) {
            assertThrows(
                    InvalidCredentialsException.class, () -> account.confirmCredentials(presented, PASSWORD_HASHER));
            assertThrows(
                    InvalidCredentialsException.class,
                    () -> account.changePassword(presented, new PlainPassword("NewPassword2@"), PASSWORD_HASHER));
            assertThrows(
                    InvalidCredentialsException.class,
                    () -> account.authorizeEmailChange(presented, new Email("new@example.com"), PASSWORD_HASHER));
            assertThrows(InvalidCredentialsException.class, () -> account.close(presented, PASSWORD_HASHER));
        }
        assertEquals(AccountStatus.ACTIVE, account.status());
        assertEquals(ORIGINAL_UPDATED_AT, account.updatedAt());
    }

    @Test
    void confirming_credentials_with_the_right_password_on_an_active_account_changes_nothing() {
        UserAccount account = account(AccountStatus.ACTIVE);

        account.confirmCredentials(PRESENTED_PASSWORD, PASSWORD_HASHER);

        assertEquals(AccountStatus.ACTIVE, account.status());
        assertEquals(ORIGINAL_UPDATED_AT, account.updatedAt());
        assertTrue(PASSWORD_HASHER.matches(PASSWORD, account.passwordHash()));
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void close_treats_a_policy_failing_password_as_wrong_for_every_status(AccountStatus status) {
        UserAccount account = account(status);

        assertThrows(InvalidCredentialsException.class, () -> account.close(PRESENTED_WEAK_PASSWORD, PASSWORD_HASHER));

        assertEquals(status, account.status());
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void require_accessible_refuses_exactly_the_non_active_statuses(AccountStatus status) {
        UserAccount account = account(status);

        if (status == AccountStatus.ACTIVE) {
            account.requireAccessible("some operation");
        } else {
            assertThrows(
                    InvalidAccountStateTransitionException.class, () -> account.requireAccessible("some operation"));
        }
    }

    private static final class CountingHasher implements PasswordHasher {

        private final PasswordHasher delegate = new TestPasswordHasher();
        private int matchCalls;

        @Override
        public PasswordHash hash(PlainPassword password) {
            return delegate.hash(password);
        }

        @Override
        public boolean matches(PlainPassword password, PasswordHash passwordHash) {
            matchCalls++;
            return delegate.matches(password, passwordHash);
        }
    }

    private static final class TestPasswordHasher implements PasswordHasher {

        @Override
        public PasswordHash hash(PlainPassword password) {
            return new PasswordHash("test", String.valueOf(password.characters()), new byte[] {1});
        }

        @Override
        public boolean matches(PlainPassword password, PasswordHash passwordHash) {
            return passwordHash.hash().equals(String.valueOf(password.characters()));
        }
    }
}
