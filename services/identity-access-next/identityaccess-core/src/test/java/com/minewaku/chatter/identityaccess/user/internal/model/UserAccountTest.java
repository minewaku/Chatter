package com.minewaku.chatter.identityaccess.user.internal.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.user.internal.exception.InvalidAccountStateTransitionException;
import com.minewaku.chatter.identityaccess.user.internal.exception.InvalidCredentialsException;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class UserAccountTest {

    private static final PlainPassword PASSWORD = new PlainPassword("Password1!");
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
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
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
                () -> account.changePassword(PASSWORD, new PlainPassword("NewPassword2@"), PASSWORD_HASHER));
        assertThrows(
                InvalidAccountStateTransitionException.class,
                () -> account.changeUsername(PASSWORD, new Username("new_person"), PASSWORD_HASHER));
        assertThrows(
                InvalidAccountStateTransitionException.class,
                () -> account.changeEmail(PASSWORD, new Email("new@example.com"), PASSWORD_HASHER));
    }

    @Test
    void password_changes_require_accessible_correct_credentials_and_a_new_value() {
        UserAccount account = account(AccountStatus.ACTIVE);
        PlainPassword newPassword = new PlainPassword("NewPassword2@");

        assertTrue(account.changePassword(PASSWORD, newPassword, PASSWORD_HASHER));
        assertTrue(PASSWORD_HASHER.matches(newPassword, account.passwordHash()));
        assertFalse(PASSWORD_HASHER.matches(PASSWORD, account.passwordHash()));
        assertFalse(account.changePassword(newPassword, newPassword, PASSWORD_HASHER));
        assertThrows(
                InvalidCredentialsException.class,
                () -> account.changePassword(
                        new PlainPassword("Incorrect2@"), new PlainPassword("OtherPassword3#"), PASSWORD_HASHER));
        assertThrows(InvalidAccountStateTransitionException.class, () -> account(AccountStatus.LOCKED)
                .changePassword(PASSWORD, new PlainPassword("OtherPassword3#"), PASSWORD_HASHER));
    }

    @Test
    void username_changes_require_accessible_correct_credentials_and_a_new_value() {
        UserAccount account = account(AccountStatus.ACTIVE);

        assertTrue(account.changeUsername(PASSWORD, new Username("new_person"), PASSWORD_HASHER));
        assertEquals("new_person", account.username().value());
        assertFalse(account.changeUsername(PASSWORD, new Username("new_person"), PASSWORD_HASHER));
        assertThrows(
                InvalidCredentialsException.class,
                () -> account.changeUsername(
                        new PlainPassword("Incorrect2@"), new Username("other_person"), PASSWORD_HASHER));
        assertThrows(InvalidAccountStateTransitionException.class, () -> account(AccountStatus.SUSPENDED)
                .changeUsername(PASSWORD, new Username("other_person"), PASSWORD_HASHER));
    }

    @Test
    void email_changes_require_accessible_correct_credentials_and_a_new_normalized_value() {
        UserAccount account = account(AccountStatus.ACTIVE);

        assertFalse(account.changeEmail(PASSWORD, new Email(" PERSON@example.com "), PASSWORD_HASHER));
        assertTrue(account.changeEmail(PASSWORD, new Email("New@Example.com"), PASSWORD_HASHER));
        assertEquals("new@example.com", account.email().value());
        assertEquals(AccountStatus.PENDING_VERIFICATION, account.status());
        assertThrows(
                InvalidAccountStateTransitionException.class,
                () -> account.changeEmail(PASSWORD, new Email("other@example.com"), PASSWORD_HASHER));
        assertThrows(InvalidCredentialsException.class, () -> account(AccountStatus.ACTIVE)
                .changeEmail(new PlainPassword("Incorrect2@"), new Email("other@example.com"), PASSWORD_HASHER));
    }

    @Test
    void value_objects_reject_invalid_values() {
        assertThrows(IllegalArgumentException.class, () -> new UserId(0));
        assertThrows(IllegalArgumentException.class, () -> new Email("not-an-email"));
        assertThrows(IllegalArgumentException.class, () -> new Username(".bad"));
        assertThrows(IllegalArgumentException.class, () -> new PlainPassword("password"));
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
