package com.minewaku.chatter.profile.presentation.web.api.command;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.minewaku.chatter.profile.application.port.inbound.command.profile.command.InputAvatar;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.command.InputBanner;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.GenerateUploadSignatureUseCase;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.UpdateProfileUseCase;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.UploadAvatarUseCase;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.UploadBannerUseCase;
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
	private final UploadAvatarUseCase uploadAvatarUseCase;
	private final UploadBannerUseCase uploadBannerUseCase;

	public ProfileCommandController(
			GenerateUploadSignatureUseCase generateUploadSignatureUseCase,
			UpdateProfileUseCase updateProfileUseCase,
			UploadAvatarUseCase uploadAvatarUseCase,
			UploadBannerUseCase uploadBannerUseCase) {

		this.generateUploadSignatureUseCase = generateUploadSignatureUseCase;
		this.updateProfileUseCase = updateProfileUseCase;
		this.uploadAvatarUseCase = uploadAvatarUseCase;
		this.uploadBannerUseCase = uploadBannerUseCase;
	}

	
	@PostMapping("/upload-signature")
	public ResponseEntity<AssetStorage.Response> generateUploadSignature(
				@RequestBody String namespace,
				@AuthenticationPrincipal Jwt jwt) {

		Namespace ns = Namespace.valueOf(namespace);
		GenerateUploadSignatureUseCase.Command command = new GenerateUploadSignatureUseCase.Command(ns);
		generateUploadSignatureUseCase.handle(command);

		AssetStorage.Response response = generateUploadSignatureUseCase.handle(command);
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

	@PostMapping("/@me/avatar")
	public ResponseEntity<Void> uploadAvatar(
				@RequestParam("file") MultipartFile file, 
				@AuthenticationPrincipal Jwt jwt) {

		InputAvatar inputAvatar = new InputAvatar(
			file.getOriginalFilename(),
			file.getContentType(),
			file.getSize(),
			() -> file.getInputStream()
		);

		UploadAvatarUseCase.Command command = new UploadAvatarUseCase.Command(
			new ProfileId(Long.parseLong(jwt.getSubject())),
			inputAvatar
		);
		
		uploadAvatarUseCase.handle(command);

		return ResponseEntity.ok().build();
	}

	@PostMapping("/@me/banner")
	public ResponseEntity<Void> uploadBanner(
				@RequestParam("file") MultipartFile file, 
				@AuthenticationPrincipal Jwt jwt) {

		InputBanner inputBanner = new InputBanner(
			file.getOriginalFilename(),
			file.getContentType(),
			file.getSize(),
			() -> file.getInputStream()
		);

		UploadBannerUseCase.Command command = new UploadBannerUseCase.Command(
			new ProfileId(Long.parseLong(jwt.getSubject())),
			inputBanner
		);
		
		uploadBannerUseCase.handle(command);

		return ResponseEntity.ok().build();
	}

	
}
