package com.minewaku.chatter.profilenext.presentation.profile.web;

import com.minewaku.chatter.profilenext.presentation.image.ImageUrlResolver;
import com.minewaku.chatter.profilenext.presentation.profile.dto.ProfileResponse;
import com.minewaku.chatter.profilenext.presentation.profile.dto.SaveProfileRequest;
import com.minewaku.chatter.profilenext.presentation.shared.error.InvalidIdentifierException;
import com.minewaku.chatter.profilenext.presentation.shared.security.AccessPrincipal;
import com.minewaku.chatter.profilenext.profile.api.command.SaveProfileUseCase;
import com.minewaku.chatter.profilenext.profile.api.query.GetProfileUseCase;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private static final Pattern POSITIVE_WHOLE_NUMBER = Pattern.compile("[1-9][0-9]*");

    private final GetProfileUseCase getProfile;
    private final SaveProfileUseCase saveProfile;
    private final ImageUrlResolver urls;

    @GetMapping("/@me")
    public ProfileResponse readMine(@AuthenticationPrincipal Jwt jwt) {
        return read(AccessPrincipal.from(jwt).userId());
    }

    @GetMapping("/{userId}")
    public ProfileResponse readOne(@PathVariable String userId) {
        return read(parseUserId(userId));
    }

    /** The caller is always the token's subject; no path or body field can name another user. */
    @PatchMapping("/@me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveMine(@AuthenticationPrincipal Jwt jwt, @RequestBody SaveProfileRequest request) {
        saveProfile.handle(new SaveProfileUseCase.Command(
                AccessPrincipal.from(jwt).userId(), request.displayName(), request.bio()));
    }

    private ProfileResponse read(long userId) {
        return ProfileResponse.from(getProfile.handle(new GetProfileUseCase.Query(userId)), urls);
    }

    private static long parseUserId(String value) {
        if (!POSITIVE_WHOLE_NUMBER.matcher(value).matches()) {
            throw new InvalidIdentifierException("userId");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new InvalidIdentifierException("userId");
        }
    }
}
