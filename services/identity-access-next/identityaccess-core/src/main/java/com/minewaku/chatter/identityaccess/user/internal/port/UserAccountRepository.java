package com.minewaku.chatter.identityaccess.user.internal.port;
import java.util.*; import org.springframework.data.repository.CrudRepository; import com.minewaku.chatter.identityaccess.user.internal.model.*;
public interface UserAccountRepository extends CrudRepository<UserAccount,UserId>{ Optional<UserAccount> findByEmail(Email email); }