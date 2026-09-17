package com.minewaku.chatter.identityaccess.user.internal.model;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
class UserAccountTest {
 private UserAccount pending(){return UserAccount.register(new UserId(42),new Email("Person@Example.com"),new Username("person_42"),new Birthday(LocalDate.of(1990,1,1)),new PasswordHash("argon2id","hash",new byte[]{1}));}
 @Test void registration_is_pending_and_inaccessible(){var a=pending();assertEquals(AccountStatus.PENDING_VERIFICATION,a.status());assertFalse(a.isAccessible());assertEquals("person@example.com",a.email().value());}
 @Test void state_machine_obeys_account_rules(){var a=pending();assertTrue(a.activateAfterVerification());assertTrue(a.lock());assertTrue(a.unlock());assertEquals(AccountStatus.ACTIVE,a.status());assertTrue(a.disable());assertTrue(a.enable());assertEquals(AccountStatus.ACTIVE,a.status());}
 @Test void delete_is_terminal_anonymous_and_frees_original_values(){var a=pending();assertTrue(a.softDelete());assertEquals(AccountStatus.DELETED,a.status());assertNotEquals("person@example.com",a.email().value());assertNotEquals("person_42",a.username().value());assertThrows(InvalidAccountStateTransitionException.class,a::enable);assertFalse(a.softDelete());}
 @Test void value_objects_reject_invalid_values(){assertThrows(IllegalArgumentException.class,()->new UserId(0));assertThrows(IllegalArgumentException.class,()->new Email("not-an-email"));assertThrows(IllegalArgumentException.class,()->new Username(".bad"));assertThrows(IllegalArgumentException.class,()->new PlainPassword("password"));}
}