package com.minewaku.chatter.profilenext.profile.internal.application.command;

import com.minewaku.chatter.profilenext.profile.api.command.SaveProfileUseCase;
import com.minewaku.chatter.profilenext.profile.internal.model.Bio;
import com.minewaku.chatter.profilenext.profile.internal.model.DisplayName;
import com.minewaku.chatter.profilenext.profile.internal.model.ProfileEdit;
import com.minewaku.chatter.profilenext.profile.internal.model.UserId;
import com.minewaku.chatter.profilenext.profile.internal.port.repository.ProfileRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Validates only the fields that were sent, then writes them in one statement. Nothing sent means nothing written. */
@Service
@Transactional
public class SaveProfileService implements SaveProfileUseCase {

    private final ProfileRepository profiles;
    private final Clock clock;

    public SaveProfileService(ProfileRepository profiles, Clock clock) {
        this.profiles = profiles;
        this.clock = clock;
    }

    @Override
    public Result handle(Command command) {
        UserId userId = UserId.of(command.userId());
        ProfileEdit edit = new ProfileEdit(
                command.displayName() == null ? null : DisplayName.of(command.displayName()),
                command.bio() == null ? null : Bio.of(command.bio()));
        if (edit.isEmpty()) {
            return new Result(false);
        }
        boolean written = profiles.applyEdit(userId, edit.displayName(), edit.bio(), clock.instant());
        return new Result(written);
    }
}
