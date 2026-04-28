package com.minewaku.chatter.profile.presentation.web.api.command;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.GenerateUploadSignatureUseCase;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.UpdateProfileUseCase;
import com.minewaku.chatter.profile.application.port.outbound.storage.AssetStorage;
import com.minewaku.chatter.profile.domain.model.file.model.Namespace;
import com.minewaku.chatter.profile.domain.model.profile.model.Bio;
import com.minewaku.chatter.profile.domain.model.profile.model.DisplayName;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.presentation.web.request.UpdateProfileRequest;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Profile Command", description = "Profile Command API")
@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileCommandController {

	private final GenerateUploadSignatureUseCase generateUploadSignatureUseCase;
	private final UpdateProfileUseCase updateProfileUseCase;

	public ProfileCommandController(
			GenerateUploadSignatureUseCase generateUploadSignatureUseCase,
			UpdateProfileUseCase updateProfileUseCase) {

		this.generateUploadSignatureUseCase = generateUploadSignatureUseCase;
		this.updateProfileUseCase = updateProfileUseCase;
	}

	@PostMapping("/avatar/upload-signature")
	public ResponseEntity<AssetStorage.UploadSignature> generateAvatarUploadSignature (
				@AuthenticationPrincipal Jwt jwt) {

		Map<String, Object> params = Map.of("profileId", jwt.getSubject());
		Namespace namespace = Namespace.USER_AVATARS;
		GenerateUploadSignatureUseCase.Command command = new GenerateUploadSignatureUseCase.Command(namespace, params);
		generateUploadSignatureUseCase.handle(command);

		AssetStorage.UploadSignature response = generateUploadSignatureUseCase.handle(command);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/banner/upload-signature")
	public ResponseEntity<AssetStorage.UploadSignature> generateBannerUploadSignature (
				@AuthenticationPrincipal Jwt jwt) {

		Map<String, Object> params = Map.of("profileId", jwt.getSubject());
		Namespace namespace = Namespace.USER_BANNERS;
		GenerateUploadSignatureUseCase.Command command = new GenerateUploadSignatureUseCase.Command(namespace, params);
		generateUploadSignatureUseCase.handle(command);

		AssetStorage.UploadSignature response = generateUploadSignatureUseCase.handle(command);
		return ResponseEntity.ok(response);
	}


	@PutMapping("")
	public ResponseEntity<Void> updateProfile(
				@AuthenticationPrincipal Jwt jwt,
				@RequestBody UpdateProfileRequest request) {

		ProfileId profileId = new ProfileId(Long.parseLong(jwt.getSubject()));
		DisplayName	displayName = new DisplayName(request.displayName());
		Bio bio = new Bio(request.bio());
		
		UpdateProfileUseCase.Command command = new UpdateProfileUseCase.Command(
			profileId,
			displayName,
			bio);

		updateProfileUseCase.handle(command);
		return ResponseEntity.ok().build();
	}
	
}
