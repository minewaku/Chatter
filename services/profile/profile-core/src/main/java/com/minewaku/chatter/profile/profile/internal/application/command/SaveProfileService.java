package com.minewaku.chatter.profile.profile.internal.application.command;

import com.minewaku.chatter.profile.profile.api.command.SaveProfileUseCase;
import com.minewaku.chatter.profile.profile.internal.model.Bio;
import com.minewaku.chatter.profile.profile.internal.model.DisplayName;
import com.minewaku.chatter.profile.profile.internal.model.UserId;
import com.minewaku.chatter.profile.profile.internal.port.repository.ProfileRepository;
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
        DisplayName displayName = command.displayName() == null ? null : DisplayName.of(command.displayName());
        Bio bio = command.bio() == null ? null : Bio.of(command.bio());
        if (displayName == null && bio == null) {
            return new Result(false);
        }
        return new Result(profiles.save(userId, displayName, bio, clock.instant()));
    }
}
