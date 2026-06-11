package com.minewaku.chatter.profile.presentation.web.api.controller.command;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.profile.application.port.inbound.command.asset.usecase.GenerateUploadSignatureUseCase;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.UpdateProfileUseCase;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.profile.domain.model.profile.model.Bio;
import com.minewaku.chatter.profile.domain.model.profile.model.DisplayName;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.presentation.web.api.request.UpdateProfileRequest;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

@Tag(name = "Profile Command", description = "Profile Command API")
@RestController
@RequestMapping("/api/v1/profiles")
@AllArgsConstructor
public class ProfileCommandController {

	@Qualifier("generateUploadAvatarSignatureUseCase")
	private final GenerateUploadSignatureUseCase generateUploadAvatarSignatureUseCase;

	@Qualifier("generateUploadBannerSignatureUseCase")
	private final GenerateUploadSignatureUseCase generateUploadBannerSignatureUseCase;

	private final UpdateProfileUseCase updateProfileUseCase;


	@PostMapping("/avatar/upload-signature")
	public ResponseEntity<AssetStorage.UploadSignature> generateAvatarUploadSignature (
				@AuthenticationPrincipal Jwt jwt) {

		Map<String, Object> params = new HashMap<>(Map.of("profileId", jwt.getSubject()));
		GenerateUploadSignatureUseCase.Command command = new GenerateUploadSignatureUseCase.Command(params);
		generateUploadAvatarSignatureUseCase.handle(command);

		AssetStorage.UploadSignature response = generateUploadAvatarSignatureUseCase.handle(command);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/banner/upload-signature")
	public ResponseEntity<AssetStorage.UploadSignature> generateBannerUploadSignature (
				@AuthenticationPrincipal Jwt jwt) {

		Map<String, Object> params = new HashMap<>(Map.of("profileId", jwt.getSubject()));
		GenerateUploadSignatureUseCase.Command command = new GenerateUploadSignatureUseCase.Command(params);
		generateUploadBannerSignatureUseCase.handle(command);

		AssetStorage.UploadSignature response = generateUploadBannerSignatureUseCase.handle(command);
		return ResponseEntity.ok(response);
	}

	@PutMapping("")
	public ResponseEntity<Void> updateProfile(
				@AuthenticationPrincipal Jwt jwt,
				@RequestBody UpdateProfileRequest request) {

		ProfileId profileId = new ProfileId(Long.parseLong(jwt.getSubject()));
		DisplayName	displayName = request.displayName() == null ? null : new DisplayName(request.displayName());
		Bio bio = request.bio() == null ? null : new Bio(request.bio());
		
		UpdateProfileUseCase.Command command = new UpdateProfileUseCase.Command(
			profileId,
			displayName,
			bio);

		updateProfileUseCase.handle(command);
		return ResponseEntity.ok().build();
	}
	
}
