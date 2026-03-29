package com.minewaku.chatter.profile.presentation.web.api.query;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.profile.application.port.inbound.query.GetProfileUseCase;
import com.minewaku.chatter.profile.application.port.outbound.query.model.ProfileReadModel;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Profile Query", description = "Profile Query API")
@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileQueryController {
    
    private final GetProfileUseCase getProfileUseCase;

    public ProfileQueryController(
            GetProfileUseCase getProfileUseCase) {
                
        this.getProfileUseCase = getProfileUseCase;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileReadModel> getProfile(@PathVariable("id") Long id) {
        ProfileId profileId = new ProfileId(id);
        ProfileReadModel profile = getProfileUseCase.handle(profileId);
        
        return ResponseEntity.ok(profile);
    }
}
