package com.minewaku.chatter.profilenext.profile.internal.port.query;

import com.minewaku.chatter.profilenext.profile.internal.model.Profile;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface ProfileQueryRepository {

    Optional<Profile> findByUserId(UserId userId);

    /** Of the given public ids, those that any profile holds in either image slot. */
    Set<String> findReferencedPublicIds(Collection<String> publicIds);
}
